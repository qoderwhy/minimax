package com.qkit.system.domain.dto;

import java.io.Serializable;

/**
 * 字典项分页查询入参（管理端）。
 *
 * <p>{@code status} 为空表示不限状态（管理端需要能看到已停用项）。</p>
 */
public record DictItemQueryDTO(
        String dictType,
        String label,
        Integer status,
        Long pageNum,
        Long pageSize
) implements Serializable {

    /** 仅补齐分页默认值，保留其余查询条件 */
    public DictItemQueryDTO withPageDefaults() {
        return new DictItemQueryDTO(dictType, label, status,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
    }
}
