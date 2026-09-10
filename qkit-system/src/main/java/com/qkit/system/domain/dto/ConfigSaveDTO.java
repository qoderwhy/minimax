package com.qkit.system.domain.dto;

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record ConfigSaveDTO(
        @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
        @NotBlank(message = "参数名称不能为空", groups = SaveGroup.class) String configName,
        @NotBlank(message = "参数键名不能为空", groups = SaveGroup.class) String configKey,
        String configValue,
        String configType,
        String remark
) implements Serializable {
}
