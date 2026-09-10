package com.qkit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.RoleConvert;
import com.qkit.system.domain.dto.RoleQueryDTO;
import com.qkit.system.domain.dto.RoleSaveDTO;
import com.qkit.system.domain.entity.Role;
import com.qkit.system.domain.entity.RoleDept;
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.domain.vo.RoleVO;
import com.qkit.system.mapper.RoleDeptMapper;
import com.qkit.system.mapper.RoleMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.PermissionService;
import com.qkit.system.service.RoleMenuService;
import com.qkit.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final RoleMenuService roleMenuService;
    private final RoleConvert roleConvert;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public R<List<RoleVO>> page(RoleQueryDTO query) {
        Page<Role> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .like(StrUtil.isNotBlank(query.name()), Role::getName, query.name())
                .like(StrUtil.isNotBlank(query.code()), Role::getCode, query.code())
                .eq(query.status() != null, Role::getStatus, query.status())
                .orderByAsc(Role::getSort);
        Page<Role> result = roleMapper.selectPage(page, wrapper);
        return R.ok(roleConvert.toVOList(result.getRecords()), result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<RoleVO>> list() {
        List<Role> list = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getSort));
        return R.ok(roleConvert.toVOList(list));
    }

    @Override
    @Transactional(readOnly = true)
    public R<RoleVO> detail(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        RoleVO vo = roleConvert.toVO(role);
        List<Long> deptIds = roleDeptMapper.selectList(
                        new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, id))
                .stream().map(RoleDept::getDeptId).toList();
        return R.ok(new RoleVO(
                vo.id(), vo.name(), vo.code(), vo.dataScope(), vo.dataScopeLabel(),
                vo.sort(), vo.status(), vo.remark(), vo.createTime(), deptIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(RoleSaveDTO dto) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, dto.code()));
        if (count > 0) throw new BusinessException(ErrorCode.ROLE_EXISTS);
        if ("admin".equals(dto.code())) throw new BusinessException(ErrorCode.ROLE_SYSTEM_PROTECTED);
        Role role = roleConvert.toEntity(dto);
        if (role.getDataScope() == null) role.setDataScope(2);
        if (role.getStatus() == null) role.setStatus(1);
        roleMapper.insert(role);
        saveRoleDepts(role.getId(), dto.deptIds());
        return R.ok(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(RoleSaveDTO dto) {
        Role exist = roleMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        if ("admin".equals(exist.getCode())) throw new BusinessException(ErrorCode.ROLE_SYSTEM_PROTECTED);
        Role update = roleConvert.toEntity(dto);
        roleMapper.updateById(update);
        saveRoleDepts(dto.id(), dto.deptIds());
        clearPermCache(dto.id());
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        for (Long id : ids) {
            Role role = roleMapper.selectById(id);
            if (role != null && "admin".equals(role.getCode())) throw new BusinessException(ErrorCode.ROLE_SYSTEM_PROTECTED);
        }
        Long usedCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, ids));
        if (usedCount > 0) throw new BusinessException(ErrorCode.ROLE_IN_USE);
        roleMapper.deleteBatchIds(ids);
        for (Long id : ids) clearPermCache(id);
        return R.ok(true);
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<Long>> getMenuIds(Long roleId) {
        return R.ok(roleMenuService.getMenuIdsByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> assignMenu(Long roleId, List<Long> menuIds) {
        roleMenuService.saveByRoleId(roleId, menuIds);
        clearPermCache(roleId);
        return R.ok(true);
    }

    private void clearPermCache(Long roleId) {
        List<Long> userIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                .stream().map(UserRole::getUserId).toList();
        userIds.forEach(permissionService::clearUserPermissionCache);
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<Long>> getDeptIds(Long roleId) {
        List<Long> deptIds = roleDeptMapper.selectList(
                new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, roleId))
                .stream().map(RoleDept::getDeptId).toList();
        return R.ok(deptIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> assignDept(Long roleId, List<Long> deptIds) {
        saveRoleDepts(roleId, deptIds);
        clearPermCache(roleId);
        return R.ok(true);
    }

    private void saveRoleDepts(Long roleId, List<Long> deptIds) {
        if (roleId == null) return;
        roleDeptMapper.delete(new LambdaQueryWrapper<RoleDept>().eq(RoleDept::getRoleId, roleId));
        if (deptIds == null || deptIds.isEmpty()) return;
        deptIds.stream().distinct().forEach(deptId -> {
            RoleDept rd = new RoleDept();
            rd.setRoleId(roleId);
            rd.setDeptId(deptId);
            roleDeptMapper.insert(rd);
        });
    }
}
