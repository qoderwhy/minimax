package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应 VO")
public record LoginVO(String token, Long userId, String username, String nickname) {
}
