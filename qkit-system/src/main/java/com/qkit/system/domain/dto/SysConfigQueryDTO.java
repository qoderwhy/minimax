package com.qkit.system.domain.dto;

import java.io.Serializable;

public record SysConfigQueryDTO(
        String configKey,
        String configName,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static SysConfigQueryDTO of(Long pageNum, Long pageSize) {
        return new SysConfigQueryDTO(null, null, pageNum, pageSize);
    }
}