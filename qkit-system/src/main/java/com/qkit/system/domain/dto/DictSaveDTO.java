package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record DictSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "字典名称不能为空", groups = SaveGroup.class) String name,
        @NotBlank(message = "字典类型不能为空", groups = SaveGroup.class) String type,
        Integer status,
        String remark
) implements Serializable {
}
