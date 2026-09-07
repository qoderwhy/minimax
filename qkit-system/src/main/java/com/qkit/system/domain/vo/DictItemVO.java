package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字典项 VO")
public record DictItemVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String dictType,
        String label,
        String value,
        Integer sort,
        Integer status,
        String cssClass,
        String remark
) {
}
