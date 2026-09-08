package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "系统参数配置 VO")
public record SysConfigVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String configName,
        String configKey,
        String configValue,
        String configType,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}