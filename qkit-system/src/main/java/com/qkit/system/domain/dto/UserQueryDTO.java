package com.qkit.system.domain.dto;

import java.io.Serializable;

public record UserQueryDTO(
        String username,
        String nickname,
        String phone,
        Integer status,
        Long deptId,
        Long pageNum,
        Long pageSize
) implements Serializable {

    /** 仅补齐分页默认值，保留其余查询条件 */
    public UserQueryDTO withPageDefaults() {
        return new UserQueryDTO(username, nickname, phone, status, deptId,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}
