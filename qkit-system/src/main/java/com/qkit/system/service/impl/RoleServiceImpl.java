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
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.domain.vo.RoleVO;
import com.qkit.system.mapper.RoleMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.RoleMenuService;
import com.qkit.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RoleMenuService roleMenuService;
    private final RoleConvert roleConvert;
    private final RedisTemplate<String, Object> redisTemplate;

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
                .eq(Role::getStatus, 0)
                .orderByAsc(Role::getSort));
        return R.ok(roleConvert.toVOList(list));
    }

    @Override
    @Transactional(readOnly = true)
    public R<RoleVO> detail(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        return R.ok(roleConvert.toVO(role));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(RoleSaveDTO dto) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, dto.code()));
        if (count > 0) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        Role role = roleConvert.toEntity(dto);
        if (role.getDataScope() == null) role.setDataScope(4);
        if (role.getStatus() == null) role.setStatus(0);
        roleMapper.insert(role);
        return R.ok(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(RoleSaveDTO dto) {
        Role exist = roleMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        Role update = roleConvert.toEntity(dto);
        roleMapper.updateById(update);
        clearPermCache();
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        Long usedCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, ids));
        if (usedCount > 0) throw new BusinessException(ErrorCode.ROLE_IN_USE);
        roleMapper.deleteBatchIds(ids);
        clearPermCache();
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> assignMenu(Long roleId, List<Long> menuIds) {
        roleMenuService.saveByRoleId(roleId, menuIds);
        clearPermCache();
        return R.ok(true);
    }

    private void clearPermCache() {
        // 简化：实际项目应查询该角色下的所有用户并逐个清理 perm:* 缓存
    }
}
