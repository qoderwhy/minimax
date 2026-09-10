package com.qkit.system.domain.dto;

import java.io.Serializable;

public record SysConfigQueryDTO(
        String configKey,
        String configName,
        Long pageNum,
        Long pageSize
) implements Serializable {

    /** 仅补齐分页默认值，保留其余查询条件 */
    public SysConfigQueryDTO withPageDefaults() {
        return new SysConfigQueryDTO(configKey, configName,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}