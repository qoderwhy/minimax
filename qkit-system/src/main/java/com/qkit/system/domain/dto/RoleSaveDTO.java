package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;

public record RoleSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "角色名称不能为空", groups = SaveGroup.class) String name,
        @NotBlank(message = "角色编码不能为空", groups = SaveGroup.class) String code,
        Integer dataScope,
        Integer sort,
        Integer status,
        String remark,
        List<Long> deptIds
) implements Serializable {
}
