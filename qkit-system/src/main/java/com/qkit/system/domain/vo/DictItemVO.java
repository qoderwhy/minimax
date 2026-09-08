package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字典项 VO")
public record DictItemVO(
        Long id,
        String dictType,
        String label,
        String value,
        Integer sort,
        Integer status,
        String cssClass,
        String remark
) {
}