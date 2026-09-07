package com.qkit.system.domain.dto;

import java.io.Serializable;

public record RoleQueryDTO(
        String name,
        String code,
        Integer status,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static RoleQueryDTO of(Long pageNum, Long pageSize) {
        return new RoleQueryDTO(null, null, null, pageNum, pageSize);
    }
}
