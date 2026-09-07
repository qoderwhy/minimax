package com.qkit.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "部门树节点")
public record DeptTreeVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long parentId,
        String label,
        String value,
        List<DeptTreeVO> children
) {
    public static DeptTreeVO from(Long id, Long parentId, String name, List<DeptTreeVO> children) {
        return new DeptTreeVO(id, parentId, name, String.valueOf(id), children);
    }
}
