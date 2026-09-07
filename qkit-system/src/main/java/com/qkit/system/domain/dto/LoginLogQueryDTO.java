package com.qkit.system.domain.dto;

import java.io.Serializable;

public record LoginLogQueryDTO(
        String username,
        Integer status,
        String beginTime,
        String endTime,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static LoginLogQueryDTO of(Long pageNum, Long pageSize) {
        return new LoginLogQueryDTO(null, null, null, null, pageNum, pageSize);
    }
}
