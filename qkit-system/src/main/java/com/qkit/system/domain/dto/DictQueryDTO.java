package com.qkit.system.domain.dto;

import java.io.Serializable;

public record DictQueryDTO(
        String name,
        String type,
        Integer status,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static DictQueryDTO of(Long pageNum, Long pageSize) {
        return new DictQueryDTO(null, null, null, pageNum, pageSize);
    }
}
