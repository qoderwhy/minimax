package com.qkit.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record UserResetPasswordDTO(
        @NotNull(message = "用户ID不能为空") Long userId,
        @NotBlank(message = "新密码不能为空") String newPassword
) implements Serializable {
}