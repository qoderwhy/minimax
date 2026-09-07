package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "登录日志 VO")
public record LoginLogVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String username,
        String ip,
        String userAgent,
        Integer status,
        String message,
        LocalDateTime loginTime
) {
}
