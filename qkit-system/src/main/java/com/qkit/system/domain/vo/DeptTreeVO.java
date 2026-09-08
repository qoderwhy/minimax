package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "部门树节点")
public record DeptTreeVO(
        Long id,
        Long parentId,
        String label,
        String value,
        List<DeptTreeVO> children
) {
    public static DeptTreeVO from(Long id, Long parentId, String name, List<DeptTreeVO> children) {
        return new DeptTreeVO(id, parentId, name, String.valueOf(id), children);
    }
}