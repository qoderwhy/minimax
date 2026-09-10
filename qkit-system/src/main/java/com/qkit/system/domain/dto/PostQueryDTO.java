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

    /** 仅补齐分页默认值，保留其余查询条件 */
    public PostQueryDTO withPageDefaults() {
        return new PostQueryDTO(code, name, deptId, status,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}
