package com.qkit.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 个人资料更新 DTO。用户名、部门等字段不允许本人修改，仅暴露可编辑字段。
 */
public record UserProfileUpdateDTO(
        @Size(max = 30, message = "昵称长度不能超过30") String nickname,
        @Size(max = 30, message = "真实姓名长度不能超过30") String realName,
        @Email(message = "邮箱格式不正确") String email,
        String phone,
        @Size(max = 255, message = "头像地址长度不能超过255") String avatar,
        Integer sex,
        @Size(max = 500, message = "备注长度不能超过500") String remark
) implements Serializable {
}
