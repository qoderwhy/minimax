package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "角色 VO")
public record RoleVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        String code,
        Integer dataScope,
        String dataScopeLabel,
        Integer sort,
        Integer status,
        String remark,
        LocalDateTime createTime
) {
}
