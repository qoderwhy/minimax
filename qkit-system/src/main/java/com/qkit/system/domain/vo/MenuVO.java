package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "菜单 VO")
public record MenuVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        String type,
        @JsonSerialize(using = ToStringSerializer.class) Long parentId,
        String path,
        String component,
        String perm,
        String icon,
        Integer sort,
        Integer visible,
        Integer status,
        List<MenuVO> children
) {
}
