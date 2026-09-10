package com.qkit.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.qkit.common.cache.CacheService;
import com.qkit.system.convert.DictConvert;
import com.qkit.system.convert.DictItemConvert;
import com.qkit.system.domain.entity.Dict;
import com.qkit.system.domain.entity.DictItem;
import com.qkit.system.domain.vo.DictItemVO;
import com.qkit.system.mapper.DictItemMapper;
import com.qkit.system.mapper.DictMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典缓存装配单元测试。
 *
 * <p>重点覆盖：缓存命中不回源、未命中回源并写入缓存，以及只装配「启用」数据
 * （status=1，此处通过条件参数断言，避免依赖 lambda 列名解析）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("字典缓存装配")
class DictServiceImplTest {

    private static final String TYPE = "sys_user_sex";
    private static final String CACHE_KEY = "sys_dict:" + TYPE;

    @Mock
    private DictMapper dictMapper;
    @Mock
    private DictItemMapper dictItemMapper;
    @Mock
    private DictConvert dictConvert;
    @Mock
    private DictItemConvert dictItemConvert;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private DictServiceImpl dictService;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<DictItem>> itemWrapperCaptor;

    /**
     * LambdaQueryWrapper 解析列名依赖 TableInfo 缓存，纯单测环境下需要手动初始化，
     * 否则调用 getSqlSegment() 会因找不到实体元信息而抛异常。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DictItem.class);
        TableInfoHelper.initTableInfo(assistant, Dict.class);
    }

    @Test
    @DisplayName("缓存命中时直接返回，不再查库")
    void cacheHitDoesNotQueryDatabase() {
        DictItemVO cached = dictItemVO();
        when(cacheService.get(CACHE_KEY)).thenReturn(List.of(cached));

        assertThat(dictService.getItemsByType(TYPE)).containsExactly(cached);
        verify(dictItemMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("缓存未命中时回源数据库并写入 7 天缓存")
    void cacheMissLoadsFromDatabaseAndCaches() {
        when(cacheService.get(CACHE_KEY)).thenReturn(null);
        when(dictItemMapper.selectList(any())).thenReturn(List.of(new DictItem()));
        DictItemVO vo = dictItemVO();
        when(dictItemConvert.toVOList(any())).thenReturn(List.of(vo));

        assertThat(dictService.getItemsByType(TYPE)).containsExactly(vo);
        verify(cacheService).set(eq(CACHE_KEY), eq(List.of(vo)), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("装配字典项缓存时只取启用项")
    void onlyEnabledItemsAreLoaded() {
        when(cacheService.get(anyString())).thenReturn(null);
        when(dictItemMapper.selectList(itemWrapperCaptor.capture())).thenReturn(List.of());
        when(dictItemConvert.toVOList(any())).thenReturn(List.of());

        dictService.getItemsByType(TYPE);

        LambdaQueryWrapper<DictItem> wrapper = itemWrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("dict_type", "status");
        // 参数值包含业务类型与 1（status=1 启用），确保确实按「启用」筛选
        assertThat(wrapper.getParamNameValuePairs().values()).contains(TYPE, 1);
    }

    @Test
    @DisplayName("启动预热只加载启用的字典类型")
    @SuppressWarnings("unchecked")
    void preloadOnlyEnabledDictTypes() {
        ArgumentCaptor<LambdaQueryWrapper<Dict>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(dictMapper.selectList(captor.capture())).thenReturn(List.of());

        dictService.loadAllToCache();

        assertThat(captor.getValue().getSqlSegment()).contains("status");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(1);
    }

    private DictItemVO dictItemVO() {
        return new DictItemVO(1L, TYPE, "男", "1", 1, 1, "primary", null);
    }
}
