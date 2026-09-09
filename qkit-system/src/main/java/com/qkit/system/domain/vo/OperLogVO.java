package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "操作日志 VO")
public record OperLogVO(
        Long id,
        String module,
        String name,
        Long userId,
        String username,
        String ip,
        String userAgent,
        String method,
        String requestUrl,
        String requestMethod,
        String requestParam,
        String responseResult,
        Integer status,
        String errorMsg,
        Long costMs,
        LocalDateTime operTime
) {
}