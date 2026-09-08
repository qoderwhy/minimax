package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "登录日志 VO")
public record LoginLogVO(
        Long id,
        Long userId,
        String username,
        String ip,
        String userAgent,
        Integer status,
        String message,
        LocalDateTime loginTime
) {
}