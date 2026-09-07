package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record MenuSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "菜单名称不能为空", groups = SaveGroup.class) String name,
        @NotBlank(message = "菜单类型不能为空", groups = SaveGroup.class) String type,
        Long parentId,
        String path,
        String component,
        String perm,
        String icon,
        Integer sort,
        Integer visible,
        Integer status
) implements Serializable {
}
