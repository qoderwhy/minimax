package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "登录用户信息 VO")
public record LoginUserVO(
        Long userId,
        String username,
        String nickname,
        String realName,
        String avatar,
        Long deptId,
        String deptName,
        List<String> roles,
        List<String> permissions
) {
}