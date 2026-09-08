package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "部门下拉 VO")
public record DeptSimpleVO(
        Long id,
        String name,
        Long parentId
) {
}