package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "路由元数据")
public record RouteMetaVO(
        String title,
        String icon,
        Boolean hidden,
        Boolean keepAlive,
        String perm
) {
}
