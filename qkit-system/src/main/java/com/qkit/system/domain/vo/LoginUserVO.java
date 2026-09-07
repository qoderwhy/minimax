package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "登录用户信息 VO")
public record LoginUserVO(
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String username,
        String nickname,
        String realName,
        String avatar,
        @JsonSerialize(using = ToStringSerializer.class) Long deptId,
        String deptName,
        List<String> roles,
        List<String> permissions
) {
}
