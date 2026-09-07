package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "字典分类 VO")
public record DictVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        String type,
        Integer status,
        String remark,
        LocalDateTime createTime
) {
}
