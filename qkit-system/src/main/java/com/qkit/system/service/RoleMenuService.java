package com.qkit.system.service;

import com.qkit.system.domain.entity.RoleMenu;
import com.qkit.system.domain.entity.UserRole;

import java.util.List;

public interface RoleMenuService {
    void saveByRoleId(Long roleId, List<Long> menuIds);

    List<Long> getMenuIdsByRoleId(Long roleId);
}
