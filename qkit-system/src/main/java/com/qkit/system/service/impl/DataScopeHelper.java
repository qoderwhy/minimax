package com.qkit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.entity.RoleDept;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.enums.DataScopeEnum;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.RoleDeptMapper;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    private final PermissionService permissionService;
    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleDeptMapper roleDeptMapper;

    public List<Long> visibleDeptIds(Long userId) {
        DataScopeEnum scope = permissionService.getDataScope(userId);
        return switch (scope) {
            case ALL -> null;
            case DEPT, SELF -> List.of(currentUserDeptId(userId));
            case DEPT_AND_CHILD -> getDeptAndChildren(currentUserDeptId(userId));
            case CUSTOM -> getCustomDeptIds(userId);
        };
    }

    public List<Long> visibleUserIds(Long userId) {
        DataScopeEnum scope = permissionService.getDataScope(userId);
        return switch (scope) {
            case ALL -> null;
            case DEPT -> userIdsByDeptIds(List.of(currentUserDeptId(userId)));
            case DEPT_AND_CHILD -> userIdsByDeptIds(getDeptAndChildren(currentUserDeptId(userId)));
            case SELF -> List.of(userId);
            case CUSTOM -> userIdsByDeptIds(getCustomDeptIds(userId));
        };
    }

    public boolean isAll(Long userId) {
        return permissionService.getDataScope(userId) == DataScopeEnum.ALL;
    }

    public boolean isSelf(Long userId) {
        return permissionService.getDataScope(userId) == DataScopeEnum.SELF;
    }

    private Long currentUserDeptId(Long userId) {
        User me = userMapper.selectById(userId);
        return (me == null || me.getDeptId() == null) ? 0L : me.getDeptId();
    }

    private List<Long> getDeptAndChildren(Long deptId) {
        if (deptId == null || deptId == 0L) return List.of();
        List<Dept> all = deptMapper.selectList(null);
        Map<Long, List<Long>> parentToChildren = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId(),
                        Collectors.mapping(Dept::getId, Collectors.toList())));
        List<Long> result = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(deptId);
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            result.add(id);
            parentToChildren.getOrDefault(id, List.of()).forEach(stack::push);
        }
        return result;
    }

    private List<Long> getCustomDeptIds(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        return roleDeptMapper.selectList(
                        new LambdaQueryWrapper<RoleDept>().in(RoleDept::getRoleId, roleIds))
                .stream().map(RoleDept::getDeptId).distinct().toList();
    }

    private List<Long> userIdsByDeptIds(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return List.of();
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>().in(User::getDeptId, deptIds))
                .stream().map(User::getId).distinct().toList();
    }
}
