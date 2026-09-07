package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.RoleQueryDTO;
import com.qkit.system.domain.dto.RoleSaveDTO;
import com.qkit.system.domain.vo.RoleVO;

import java.util.List;

public interface RoleService {

    R<List<RoleVO>> page(RoleQueryDTO query);

    R<List<RoleVO>> list();

    R<RoleVO> detail(Long id);

    R<Long> create(RoleSaveDTO dto);

    R<Boolean> update(RoleSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> assignMenu(Long roleId, List<Long> menuIds);
}
