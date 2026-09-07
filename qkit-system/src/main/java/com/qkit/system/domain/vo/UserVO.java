package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 VO")
public record UserVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String username,
        String nickname,
        String realName,
        String email,
        String phone,
        String avatar,
        Integer sex,
        String sexLabel,
        @JsonSerialize(using = ToStringSerializer.class) Long deptId,
        String deptName,
        @JsonSerialize(using = ToStringSerializer.class) Long postId,
        String postName,
        Integer status,
        String statusLabel,
        String loginIp,
        LocalDateTime loginDate,
        LocalDateTime createTime,
        List<String> roleIds
) {
}
