package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.SysConfigQueryDTO;
import com.qkit.system.domain.dto.SysConfigSaveDTO;
import com.qkit.system.domain.vo.SysConfigVO;

import java.util.List;

public interface SysConfigService {

    R<List<SysConfigVO>> page(SysConfigQueryDTO query);

    R<List<SysConfigVO>> list();

    R<Long> create(SysConfigSaveDTO dto);

    R<Boolean> update(SysConfigSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    /** 按键名取值（优先走本地缓存，未命中回源数据库） */
    String getValue(String key);

    /** 按键名取值，空值返回默认值 */
    String getValue(String key, String defaultValue);

    /** 按键名取整型值，解析失败返回默认值 */
    int getInt(String key, int defaultValue);

    /** 按键名取布尔值（true/1/Y 视为真），解析失败返回默认值 */
    boolean getBoolean(String key, boolean defaultValue);

    /** 全量加载配置到本地缓存（启动时调用） */
    void loadAllToCache();
}