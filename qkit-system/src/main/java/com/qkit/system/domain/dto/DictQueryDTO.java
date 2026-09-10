package com.qkit.system.domain.dto;

import java.io.Serializable;

public record DictQueryDTO(
        String name,
        String type,
        Integer status,
        Long pageNum,
        Long pageSize
) implements Serializable {

    /** 仅补齐分页默认值，保留其余查询条件 */
    public DictQueryDTO withPageDefaults() {
        return new DictQueryDTO(name, type, status,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}
