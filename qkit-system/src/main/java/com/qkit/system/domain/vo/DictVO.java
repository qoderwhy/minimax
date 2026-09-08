package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "字典分类 VO")
public record DictVO(
        Long id,
        String name,
        String type,
        Integer status,
        String remark,
        LocalDateTime createTime
) {
}