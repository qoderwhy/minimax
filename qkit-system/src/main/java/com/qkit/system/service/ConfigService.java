package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.ConfigQueryDTO;
import com.qkit.system.domain.dto.ConfigSaveDTO;
import com.qkit.system.domain.vo.ConfigVO;

import java.util.List;

public interface ConfigService {

    R<List<ConfigVO>> page(ConfigQueryDTO query);

    R<List<ConfigVO>> list();

    R<Long> create(ConfigSaveDTO dto);

    R<Boolean> update(ConfigSaveDTO dto);

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
