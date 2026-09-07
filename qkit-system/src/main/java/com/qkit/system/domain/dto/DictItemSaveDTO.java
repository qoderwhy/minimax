package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record DictItemSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "字典类型不能为空", groups = SaveGroup.class) String dictType,
        @NotBlank(message = "字典项名称不能为空", groups = SaveGroup.class) String label,
        @NotBlank(message = "字典项值不能为空", groups = SaveGroup.class) String value,
        Integer sort,
        Integer status,
        String cssClass,
        String remark
) implements Serializable {
}
