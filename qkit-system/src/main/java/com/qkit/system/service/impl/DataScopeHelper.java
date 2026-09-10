package com.qkit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    /** 全量部门列表缓存 key；部门增删改后由 DeptServiceImpl 失效 */
    private static final String DEPT_ALL_KEY = CacheConstants.DEPT_CHILD_KEY_PREFIX + "__all__";
    private static final Duration DEPT_ALL_TTL = Duration.ofMinutes(5);

    private final PermissionService permissionService;
    private final CacheService cacheService;
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

    /** 部门增删改后调用，失效部门列表缓存 */
    public void invalidateDeptCache() {
        cacheService.delete(DEPT_ALL_KEY);
    }

    private Long currentUserDeptId(Long userId) {
        User me = userMapper.selectById(userId);
        return (me == null || me.getDeptId() == null) ? 0L : me.getDeptId();
    }

    private List<Long> getDeptAndChildren(Long deptId) {
        if (deptId == null || deptId == 0L) return List.of();
        List<Dept> all = allDepts();
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

    /** 全量部门列表，带 5 分钟缓存，避免数据权限计算每次全表扫 */
    @SuppressWarnings("unchecked")
    private List<Dept> allDepts() {
        Object cached = cacheService.get(DEPT_ALL_KEY);
        if (cached instanceof List<?> list) {
            return (List<Dept>) (List<?>) list;
        }
        List<Dept> all = deptMapper.selectList(null);
        cacheService.set(DEPT_ALL_KEY, all, DEPT_ALL_TTL);
        return all;
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
