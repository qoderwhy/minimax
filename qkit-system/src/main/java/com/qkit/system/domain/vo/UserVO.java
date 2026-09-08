package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 VO")
public record UserVO(
        Long id,
        String username,
        String nickname,
        String realName,
        String email,
        String phone,
        String avatar,
        Integer sex,
        String sexLabel,
        Long deptId,
        String deptName,
        Long postId,
        String postName,
        Integer status,
        String statusLabel,
        String loginIp,
        LocalDateTime loginDate,
        LocalDateTime createTime,
        String remark,
        List<Long> roleIds
) {
}