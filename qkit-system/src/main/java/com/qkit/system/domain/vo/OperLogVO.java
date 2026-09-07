package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "操作日志 VO")
public record OperLogVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String module,
        String name,
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String username,
        String ip,
        String userAgent,
        String method,
        String requestUrl,
        String requestMethod,
        String requestParams,
        String responseResult,
        Integer status,
        String errorMsg,
        Long costMs,
        LocalDateTime operTime
) {
}
