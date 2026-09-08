package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.common.exception.SystemException;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.entity.Post;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.UserExportVO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.PostMapper;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.UserRoleService;
import com.qkit.system.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PostMapper postMapper;
    private final UserRoleService userRoleService;
    private final UserConvert userConvert;

    @Override
    @Transactional(readOnly = true)
    public R<List<UserVO>> page(UserQueryDTO query) {
        Page<User> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(query.username()), User::getUsername, query.username())
                .like(StrUtil.isNotBlank(query.phone()), User::getPhone, query.phone())
                .eq(query.status() != null, User::getStatus, query.status())
                .eq(query.deptId() != null, User::getDeptId, query.deptId())
                .orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> voList = result.getRecords().stream().map(this::toVOWithExtra).toList();
        return R.ok(voList, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public void export(UserQueryDTO query, HttpServletResponse response) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(query.username()), User::getUsername, query.username())
                .like(StrUtil.isNotBlank(query.phone()), User::getPhone, query.phone())
                .eq(query.status() != null, User::getStatus, query.status())
                .eq(query.deptId() != null, User::getDeptId, query.deptId())
                .orderByDesc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        List<UserExportVO> exportList = new ArrayList<>(users.size());
        for (User user : users) {
            UserVO vo = toVOWithExtra(user);
            exportList.add(UserExportVO.builder()
                    .username(vo.username())
                    .nickname(vo.nickname())
                    .realName(vo.realName())
                    .phone(vo.phone())
                    .email(vo.email())
                    .deptName(vo.deptName())
                    .postName(vo.postName())
                    .statusLabel(vo.statusLabel())
                    .createTime(vo.createTime())
                    .build());
        }
        try {
            EasyExcel.write(response.getOutputStream(), UserExportVO.class)
                    .sheet("用户列表")
                    .doWrite(exportList);
        } catch (IOException e) {
            throw new SystemException(ErrorCode.EXPORT_ERROR, "导出失败，请稍后重试", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public R<UserVO> detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(id);
        UserVO vo = toVOWithExtra(user);
        return R.ok(new UserVO(
                vo.id(), vo.username(), vo.nickname(), vo.realName(),
                vo.email(), vo.phone(), vo.avatar(), vo.sex(), vo.sexLabel(),
                vo.deptId(), vo.deptName(), vo.postId(), vo.postName(),
                vo.status(), vo.statusLabel(), vo.loginIp(), vo.loginDate(),
                vo.createTime(), vo.remark(),
                roleIds == null ? null : roleIds.stream().map(String::valueOf).toList()
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(UserSaveDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.username()));
        if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);

        User user = userConvert.toEntity(dto);
        user.setPassword(BCrypt.hashpw(dto.password()));
        if (user.getStatus() == null) user.setStatus(0);
        userMapper.insert(user);
        return R.ok(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(UserSaveDTO dto) {
        User exist = userMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        if (!exist.getUsername().equals(dto.username())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.username())
                    .ne(User::getId, dto.id()));
            if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        User update = userConvert.toUpdateEntity(dto);
        userMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        userMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> resetPassword(Long userId, String newPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);
        // 强制下线
        try {
            cn.dev33.satoken.stp.StpUtil.logout(userId);
        } catch (Exception ignored) {
        }
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> assignRole(Long userId, List<Long> roleIds) {
        userRoleService.saveByUserId(userId, roleIds);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> changePassword(Long userId, PasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (!BCrypt.checkpw(dto.oldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.newPassword()));
        userMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    @Transactional(readOnly = true)
    public R<UserVO> profile(Long userId) {
        return detail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateProfile(Long userId, UserProfileUpdateDTO dto) {
        User exist = userMapper.selectById(userId);
        if (exist == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.nickname());
        update.setRealName(dto.realName());
        update.setEmail(dto.email());
        update.setPhone(dto.phone());
        update.setAvatar(dto.avatar());
        update.setSex(dto.sex());
        update.setRemark(dto.remark());
        userMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoginInfo(Long userId, String ip) {
        User u = new User();
        u.setId(userId);
        u.setLoginIp(ip);
        u.setLoginDate(LocalDateTime.now());
        userMapper.updateById(u);
    }

    private UserVO toVOWithExtra(User user) {
        UserVO vo = userConvert.toVO(user);
        String deptName = null;
        String postName = null;
        if (user.getDeptId() != null && user.getDeptId() > 0) {
            Dept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) deptName = dept.getName();
        }
        if (user.getPostId() != null && user.getPostId() > 0) {
            Post post = postMapper.selectById(user.getPostId());
            if (post != null) postName = post.getName();
        }
        return new UserVO(
                vo.id(), vo.username(), vo.nickname(), vo.realName(),
                vo.email(), vo.phone(), vo.avatar(),
                vo.sex(), vo.sexLabel(),
                vo.deptId(), deptName, vo.postId(), postName,
                vo.status(), vo.statusLabel(),
                vo.loginIp(), vo.loginDate(), vo.createTime(), vo.remark(), null);
    }
}
