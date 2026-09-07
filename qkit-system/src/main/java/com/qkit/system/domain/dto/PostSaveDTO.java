package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record PostSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "岗位编码不能为空", groups = SaveGroup.class) String code,
        @NotBlank(message = "岗位名称不能为空", groups = SaveGroup.class) String name,
        Long deptId,
        Integer sort,
        Integer status,
        String remark
) implements Serializable {
}
