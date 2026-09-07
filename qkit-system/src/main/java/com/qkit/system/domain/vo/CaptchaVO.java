package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "图形验证码 VO")
public record CaptchaVO(String captchaId, String captchaImage) {
}
