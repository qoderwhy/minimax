package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record DeptSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "部门名称不能为空", groups = SaveGroup.class) String name,
        Long parentId,
        Integer sort,
        String leader,
        String phone,
        String email,
        Integer status
) implements Serializable {
}
