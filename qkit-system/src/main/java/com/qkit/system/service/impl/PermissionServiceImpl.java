package com.qkit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.common.constant.CacheConstants;
import com.qkit.system.domain.entity.Menu;
import com.qkit.system.domain.entity.Role;
import com.qkit.system.domain.entity.RoleMenu;
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.enums.DataScopeEnum;
import com.qkit.system.mapper.MenuMapper;
import com.qkit.system.mapper.RoleMapper;
import com.qkit.system.mapper.RoleMenuMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务实现。
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> getUserPermissions(Long userId) {
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();

        // 通过角色 → 菜单 → 权限码
        Set<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds)
        ).stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
        if (menuIds.isEmpty()) return Collections.emptyList();

        Set<String> perms = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>()
                        .in(Menu::getId, menuIds)
                        .eq(Menu::getStatus, 0)
                        .isNotNull(Menu::getPerm)
                        .ne(Menu::getPerm, "")
        ).stream().map(Menu::getPerm).collect(Collectors.toSet());
        return perms.stream().toList();
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();
        return roleMapper.selectBatchIds(roleIds).stream().map(Role::getCode).toList();
    }

    @Override
    public DataScopeEnum getDataScope(Long userId) {
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return DataScopeEnum.SELF;
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(r -> DataScopeEnum.of(r.getDataScope()))
                .min((a, b) -> Integer.compare(a.getCode(), b.getCode()))
                .orElse(DataScopeEnum.SELF);
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        redisTemplate.delete(CacheConstants.PERM_KEY_PREFIX + userId);
    }

    private List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).toList();
    }
}
