package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "系统参数配置 VO")
public record SysConfigVO(
        Long id,
        String configName,
        String configKey,
        String configValue,
        String configType,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}