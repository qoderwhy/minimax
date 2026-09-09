package com.qkit.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.common.transaction.TransactionUtils;
import com.qkit.system.convert.DictConvert;
import com.qkit.system.convert.DictItemConvert;
import com.qkit.system.domain.dto.DictItemSaveDTO;
import com.qkit.system.domain.dto.DictQueryDTO;
import com.qkit.system.domain.dto.DictSaveDTO;
import com.qkit.system.domain.entity.Dict;
import com.qkit.system.domain.entity.DictItem;
import com.qkit.system.domain.vo.DictItemVO;
import com.qkit.system.domain.vo.DictVO;
import com.qkit.system.mapper.DictItemMapper;
import com.qkit.system.mapper.DictMapper;
import com.qkit.system.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DictServiceImpl implements DictService, CommandLineRunner {

    private final DictMapper dictMapper;
    private final DictItemMapper dictItemMapper;
    private final DictConvert dictConvert;
    private final DictItemConvert dictItemConvert;
    private final CacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public R<List<DictVO>> page(DictQueryDTO query) {
        Page<Dict> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<Dict>()
                .like(StrUtil.isNotBlank(query.name()), Dict::getName, query.name())
                .like(StrUtil.isNotBlank(query.type()), Dict::getType, query.type())
                .eq(query.status() != null, Dict::getStatus, query.status())
                .orderByDesc(Dict::getId);
        Page<Dict> result = dictMapper.selectPage(page, wrapper);
        return R.ok(dictConvert.toVOList(result.getRecords()), result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    public R<List<DictItemVO>> listItems(String dictType) {
        return R.ok(getItemsByType(dictType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createDict(DictSaveDTO dto) {
        Dict dict = dictConvert.toEntity(dto);
        if (dict.getStatus() == null) dict.setStatus(1);
        dictMapper.insert(dict);
        return R.ok(dict.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateDict(DictSaveDTO dto) {
        Dict dict = dictConvert.toEntity(dto);
        dictMapper.updateById(dict);
        TransactionUtils.afterCommit(() -> loadToCache(dto.type()));
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteDict(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        List<String> cacheKeys = new ArrayList<>();
        for (Long id : ids) {
            Dict dict = dictMapper.selectById(id);
            if (dict == null) continue;
            Long itemCount = dictItemMapper.selectCount(
                    new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictType, dict.getType()));
            if (itemCount > 0) throw new BusinessException(ErrorCode.DICT_HAS_ITEMS);
            cacheKeys.add(dict.getType());
        }
        dictMapper.deleteBatchIds(ids);
        TransactionUtils.afterCommit(() -> cacheKeys.forEach(key ->
                cacheService.delete(CacheConstants.DICT_KEY_PREFIX + key)));
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createItem(DictItemSaveDTO dto) {
        DictItem item = dictItemConvert.toEntity(dto);
        if (item.getStatus() == null) item.setStatus(1);
        if (item.getSort() == null) item.setSort(0);
        dictItemMapper.insert(item);
        TransactionUtils.afterCommit(() -> loadToCache(dto.dictType()));
        return R.ok(item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateItem(DictItemSaveDTO dto) {
        DictItem item = dictItemConvert.toEntity(dto);
        dictItemMapper.updateById(item);
        TransactionUtils.afterCommit(() -> loadToCache(dto.dictType()));
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteItem(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        // 删除前查询受影响 dictType
        List<DictItem> items = dictItemMapper.selectBatchIds(ids);
        dictItemMapper.deleteBatchIds(ids);
        TransactionUtils.afterCommit(() -> items.stream().map(DictItem::getDictType).distinct().forEach(this::loadToCache));
        return R.ok(true);
    }

    @Override
    public void loadAllToCache() {
        List<Dict> dicts = dictMapper.selectList(new LambdaQueryWrapper<Dict>().eq(Dict::getStatus, 0));
        dicts.forEach(d -> loadToCache(d.getType()));
    }

    @Override
    public List<DictItemVO> getItemsByType(String type) {
        String key = CacheConstants.DICT_KEY_PREFIX + type;
        Object cached = cacheService.get(key);
        if (cached instanceof List<?> list) {
            return list.stream().map(o -> (DictItemVO) o).toList();
        }
        return loadToCache(type);
    }

    @Override
    public void run(String... args) {
        try {
            loadAllToCache();
            log.info("字典已加载到 Redis 缓存");
        } catch (Exception e) {
            log.warn("字典加载到 Redis 失败", e);
        }
    }

    private List<DictItemVO> loadToCache(String type) {
        List<DictItem> items = dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, type)
                .eq(DictItem::getStatus, 0)
                .orderByAsc(DictItem::getSort));
        List<DictItemVO> voList = dictItemConvert.toVOList(items);
        cacheService.set(CacheConstants.DICT_KEY_PREFIX + type, voList, Duration.ofDays(7));
        return voList;
    }
}
