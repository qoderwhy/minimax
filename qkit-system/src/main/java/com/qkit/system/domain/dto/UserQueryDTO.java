package com.qkit.system.domain.dto;

import java.io.Serializable;

public record UserQueryDTO(
        String username,
        String phone,
        Integer status,
        Long deptId,
        Long pageNum,
        Long pageSize
) implements Serializable {

    public static UserQueryDTO of(Long pageNum, Long pageSize) {
        return new UserQueryDTO(null, null, null, null, pageNum, pageSize);
    }
}
