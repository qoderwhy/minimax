package com.qkit.system.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record LoginDTO(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        String captchaId,
        String captchaCode
) implements Serializable {
}
