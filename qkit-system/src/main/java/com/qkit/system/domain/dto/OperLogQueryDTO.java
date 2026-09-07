package com.qkit.system.domain.dto;

import java.io.Serializable;

public record OperLogQueryDTO(
        String module,
        String username,
        Integer status,
        String beginTime,
        String endTime,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static OperLogQueryDTO of(Long pageNum, Long pageSize) {
        return new OperLogQueryDTO(null, null, null, null, null, pageNum, pageSize);
    }
}
