package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "菜单 VO")
public record MenuVO(
        Long id,
        String name,
        String type,
        Long parentId,
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