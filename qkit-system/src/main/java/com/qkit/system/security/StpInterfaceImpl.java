package com.qkit.system.security;

import cn.dev33.satoken.stp.StpInterface;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色获取实现。位于 system 模块以避免 framework → system 循环依赖。
 *
 * <p>权限列表缓存 30 分钟；变更角色/菜单权限时需清除 {@code perm:{userId}} 缓存。</p>
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    /** 超级管理员角色编码 */
    private static final String ADMIN_ROLE = "admin";
    /** 通配权限（拥有所有权限） */
    private static final String ALL_PERMISSION = "*:*:*";

    private final PermissionService permissionService;
    private final CacheService cacheService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        // 超级管理员兜底：admin 角色直接授予全部权限（绕过缓存，避免旧缓存误判）
        if (permissionService.getUserRoleCodes(userId).contains(ADMIN_ROLE)) {
            return List.of(ALL_PERMISSION);
        }
        String key = CacheConstants.PERM_KEY_PREFIX + userId;
        Object cached = cacheService.get(key);
        if (cached instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        List<String> perms = permissionService.getUserPermissions(userId);
        if (perms != null) {
            cacheService.set(key, perms, Duration.ofMinutes(30));
        }
        return perms == null ? Collections.emptyList() : perms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return permissionService.getUserRoleCodes(userId);
    }
}
