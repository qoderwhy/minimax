package com.qkit.system.domain.vo;

import com.qkit.system.domain.entity.Dept;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 部门树节点。
 *
 * <p>除层级信息外同时携带部门的可编辑字段，保证前端在树形列表上直接编辑时能完整回显，
 * 避免保存时把负责人、联系电话等字段覆盖为空。</p>
 */
@Schema(description = "部门树节点")
public record DeptTreeVO(
        Long id,
        Long parentId,
        String label,
        String value,
        Integer sort,
        String leader,
        String phone,
        String email,
        Integer status,
        List<DeptTreeVO> children
) {
    public static DeptTreeVO from(Dept dept, List<DeptTreeVO> children) {
        return new DeptTreeVO(dept.getId(), dept.getParentId(), dept.getName(), String.valueOf(dept.getId()),
                dept.getSort(), dept.getLeader(), dept.getPhone(), dept.getEmail(), dept.getStatus(), children);
    }

    /** 替换子节点并保留其余字段（数据权限过滤后重建树时使用） */
    public DeptTreeVO withChildren(List<DeptTreeVO> children) {
        return new DeptTreeVO(id, parentId, label, value, sort, leader, phone, email, status, children);
    }
}
