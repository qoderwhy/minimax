package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "岗位 VO")
public record PostVO(
        Long id,
        String code,
        String name,
        Long deptId,
        String deptName,
        Integer sort,
        Integer status,
        String remark,
        LocalDateTime createTime
) {
}