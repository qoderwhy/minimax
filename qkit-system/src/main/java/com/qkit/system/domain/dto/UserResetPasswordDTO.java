package com.qkit.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UserResetPasswordDTO(
        @NotNull(message = "用户ID不能为空") Long userId,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间") String newPassword
) implements Serializable {
}