package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "菜单路由 VO（前端动态路由）")
public record RouteVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        String path,
        String component,
        String redirect,
        RouteMetaVO meta,
        java.util.List<RouteVO> children
) {
}
