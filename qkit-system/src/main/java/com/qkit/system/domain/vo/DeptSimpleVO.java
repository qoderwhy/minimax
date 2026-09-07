package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "部门下拉 VO")
public record DeptSimpleVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        @JsonSerialize(using = ToStringSerializer.class) Long parentId
) {
}
