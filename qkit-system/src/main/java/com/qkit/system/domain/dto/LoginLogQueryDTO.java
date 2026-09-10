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

    /** 仅补齐分页默认值，保留其余查询条件 */
    public LoginLogQueryDTO withPageDefaults() {
        return new LoginLogQueryDTO(username, status, beginTime, endTime,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}
