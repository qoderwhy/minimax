package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UserSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "登录名不能为空", groups = SaveGroup.class)
        @Size(max = 30, message = "登录名长度不能超过30") String username,
        @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间", groups = SaveGroup.class) String password,
        String nickname,
        String realName,
        @Email(message = "邮箱格式不正确") String email,
        String phone,
        Integer sex,
        Long deptId,
        Long postId,
        Integer status,
        String remark
) implements Serializable {
}
