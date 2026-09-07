package com.qkit.system.service;

import com.qkit.system.domain.entity.UserRole;

import java.util.List;

public interface UserRoleService {

    void saveByUserId(Long userId, List<Long> roleIds);

    List<Long> getRoleIdsByUserId(Long userId);
}
