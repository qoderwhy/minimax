package com.qkit.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record PasswordDTO(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空") @Size(min = 8, max = 32, message = "新密码长度必须在8-32位之间") String newPassword
) implements Serializable {
}
