package com.qkit.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.common.transaction.TransactionUtils;
import com.qkit.system.convert.ConfigConvert;
import com.qkit.system.domain.dto.ConfigQueryDTO;
import com.qkit.system.domain.dto.ConfigSaveDTO;
import com.qkit.system.domain.entity.Config;
import com.qkit.system.domain.vo.ConfigVO;
import com.qkit.system.mapper.ConfigMapper;
import com.qkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统参数配置实现。
 *
 * <p>缓存策略：应用启动时全量加载到 Redis，管理后台增删改后同步刷新 Redis 实现热更新，
 * 无需重启应用；多实例部署天然一致。</p>
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService, CommandLineRunner {

    /** 是否系统内置参数，内置参数不允许删除或修改键名 */
    private static final String BUILTIN = "Y";

    private final ConfigMapper configMapper;
    private final ConfigConvert configConvert;
    private final CacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public R<List<ConfigVO>> page(ConfigQueryDTO query) {
        Page<Config> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<Config>()
                .like(StrUtil.isNotBlank(query.configKey()), Config::getConfigKey, query.configKey())
                .like(StrUtil.isNotBlank(query.configName()), Config::getConfigName, query.configName())
                .orderByDesc(Config::getId);
        Page<Config> result = configMapper.selectPage(page, wrapper);
        return R.ok(configConvert.toVOList(result.getRecords()), result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<ConfigVO>> list() {
        List<Config> configs = configMapper.selectList(new LambdaQueryWrapper<Config>()
                .orderByAsc(Config::getConfigKey));
        return R.ok(configConvert.toVOList(configs));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(ConfigSaveDTO dto) {
        checkKeyUnique(dto.configKey(), null);
        Config config = configConvert.toEntity(dto);
        if (StrUtil.isBlank(config.getConfigType())) config.setConfigType("N");
        if (config.getConfigValue() == null) config.setConfigValue("");
        configMapper.insert(config);
        TransactionUtils.afterCommit(() -> refreshCache(config.getConfigKey()));
        return R.ok(config.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(ConfigSaveDTO dto) {
        Config exist = configMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        // 内置参数不允许修改键名
        if (BUILTIN.equals(exist.getConfigType()) && !exist.getConfigKey().equals(dto.configKey())) {
            throw new BusinessException(ErrorCode.CONFIG_BUILTIN);
        }
        checkKeyUnique(dto.configKey(), dto.id());
        Config config = configConvert.toEntity(dto);
        if (config.getConfigValue() == null) config.setConfigValue("");
        String newKey = config.getConfigKey();
        if (!exist.getConfigKey().equals(newKey)) {
            // 键名被修改时，需清理旧键名的缓存
            String oldKey = exist.getConfigKey();
            configMapper.updateById(config);
            TransactionUtils.afterCommit(() -> {
                cacheService.delete(CacheConstants.CONFIG_KEY_PREFIX + oldKey);
                refreshCache(newKey);
            });
            return R.ok(true);
        }
        configMapper.updateById(config);
        TransactionUtils.afterCommit(() -> refreshCache(newKey));
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        List<String> cacheKeys = new ArrayList<>();
        for (Long id : ids) {
            Config config = configMapper.selectById(id);
            if (config == null) continue;
            if (BUILTIN.equals(config.getConfigType())) {
                throw new BusinessException(ErrorCode.CONFIG_BUILTIN);
            }
            cacheKeys.add(config.getConfigKey());
        }
        configMapper.deleteByIds(ids);
        TransactionUtils.afterCommit(() -> cacheKeys.forEach(key ->
                cacheService.delete(CacheConstants.CONFIG_KEY_PREFIX + key)));
        return R.ok(true);
    }

    @Override
    public String getValue(String key) {
        String cacheKey = CacheConstants.CONFIG_KEY_PREFIX + key;
        Object cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }
        return queryFromDb(key);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String value = getValue(key);
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getValue(key);
        if (StrUtil.isBlank(value) || !NumberUtil.isNumber(value)) return defaultValue;
        return NumberUtil.parseInt(value);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key);
        if (StrUtil.isBlank(value)) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    @Override
    public void loadAllToCache() {
        List<Config> configs = configMapper.selectList(new LambdaQueryWrapper<Config>());
        configs.forEach(c -> setCache(c.getConfigKey(), c.getConfigValue()));
    }

    /** 启动预热：与字典缓存保持同一时机（应用上下文就绪后），避免 @PostConstruct 阶段访问数据库 */
    @Override
    public void run(String... args) {
        try {
            loadAllToCache();
            log.info("系统参数已加载到 Redis 缓存");
        } catch (Exception e) {
            log.warn("系统参数加载到 Redis 缓存失败", e);
        }
    }

    /** 校验键名唯一（排除 updateId 自身） */
    private void checkKeyUnique(String configKey, Long excludeId) {
        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<Config>()
                .eq(Config::getConfigKey, configKey);
        if (excludeId != null) wrapper.ne(Config::getId, excludeId);
        if (configMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_EXISTS);
        }
    }

    /** 缓存未命中时回源数据库并回填缓存 */
    private String queryFromDb(String key) {
        Config config = configMapper.selectOne(new LambdaQueryWrapper<Config>()
                .eq(Config::getConfigKey, key)
                .last("LIMIT 1"));
        if (config == null) return null;
        setCache(key, config.getConfigValue());
        return config.getConfigValue();
    }

    /** 增删改后刷新缓存：存在则回填，不存在则清理，实现热更新 */
    private void refreshCache(String key) {
        Config config = configMapper.selectOne(new LambdaQueryWrapper<Config>()
                .eq(Config::getConfigKey, key)
                .last("LIMIT 1"));
        if (config == null) {
            cacheService.delete(CacheConstants.CONFIG_KEY_PREFIX + key);
            return;
        }
        setCache(key, config.getConfigValue());
    }

    /** 写入 Redis 缓存 */
    private void setCache(String key, String value) {
        cacheService.set(CacheConstants.CONFIG_KEY_PREFIX + key, value);
    }
}
