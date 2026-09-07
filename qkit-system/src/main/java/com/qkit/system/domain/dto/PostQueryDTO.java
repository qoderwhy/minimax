package com.qkit.system.domain.dto;

import java.io.Serializable;

public record PostQueryDTO(
        String code,
        String name,
        Long deptId,
        Integer status,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static PostQueryDTO of(Long pageNum, Long pageSize) {
        return new PostQueryDTO(null, null, null, null, pageNum, pageSize);
    }
}
