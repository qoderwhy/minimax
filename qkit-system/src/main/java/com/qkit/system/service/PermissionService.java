package com.qkit.system.service;

import com.qkit.system.enums.DataScopeEnum;

import java.util.List;

/**
 * 权限服务接口。由 qkit-system 模块实现。
 */
public interface PermissionService {

    /** 获取用户的所有权限码（menu.type=F 的 perm） */
    List<String> getUserPermissions(Long userId);

    /** 获取用户的所有角色编码 */
    List<String> getUserRoleCodes(Long userId);

    /** 获取用户的数据权限范围（取所有角色中最大权限） */
    DataScopeEnum getDataScope(Long userId);

    /** 清除某用户的权限缓存（角色/菜单变更后调用） */
    void clearUserPermissionCache(Long userId);
}
