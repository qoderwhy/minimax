package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.common.exception.SystemException;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.entity.Post;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.entity.UserPost;
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.domain.vo.UserExportVO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.PostMapper;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.mapper.UserPostMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.PermissionService;
import com.qkit.system.service.UserRoleService;
import com.qkit.system.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 内置超级管理员账号，禁止删除 / 停用 / 改名 */
    private static final String ADMIN_USERNAME = "admin";

    /** 导出单次最大行数：超过则直接失败，避免整表导出拖垮服务 */
    private static final long EXPORT_MAX_ROWS = 5000L;

    /** 导出分批读取大小，不得超过分页插件的单页上限 */
    private static final long EXPORT_BATCH_SIZE = 200L;

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PostMapper postMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserPostMapper userPostMapper;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final UserConvert userConvert;

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_user", deptColumn = "dept_id", userColumn = "create_by")
    public R<List<UserVO>> page(UserQueryDTO query) {
        Page<User> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(query.username()), User::getUsername, query.username())
                .like(StrUtil.isNotBlank(query.nickname()), User::getNickname, query.nickname())
                .like(StrUtil.isNotBlank(query.phone()), User::getPhone, query.phone())
                .eq(query.status() != null, User::getStatus, query.status())
                .eq(query.deptId() != null, User::getDeptId, query.deptId())
                .orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> voList = enrichUsers(result.getRecords());
        return R.ok(voList, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_user", deptColumn = "dept_id", userColumn = "create_by")
    public void export(UserQueryDTO query, HttpServletResponse response) {
        // 先统计命中总数：超限直接失败，避免响应已写入后报错导致文件损坏
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(query.username()), User::getUsername, query.username())
                .like(StrUtil.isNotBlank(query.nickname()), User::getNickname, query.nickname())
                .like(StrUtil.isNotBlank(query.phone()), User::getPhone, query.phone())
                .eq(query.status() != null, User::getStatus, query.status())
                .eq(query.deptId() != null, User::getDeptId, query.deptId());
        long total = userMapper.selectCount(wrapper);
        if (total > EXPORT_MAX_ROWS) {
            throw new BusinessException(ErrorCode.EXPORT_LIMIT_EXCEEDED);
        }
        // 统计完成后再补排序：selectCount 会把 order by 一并带入，MySQL 严格模式下无法执行
        wrapper.orderByDesc(User::getId);

        OutputStream outputStream;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new SystemException(ErrorCode.EXPORT_ERROR, ErrorCode.EXPORT_ERROR.getMessage(), e);
        }

        // 分批读取 + 流式写出，避免整表载入内存
        ExcelWriter writer = EasyExcel.write(outputStream, UserExportVO.class).build();
        WriteSheet sheet = EasyExcel.writerSheet("用户列表").build();
        long pageNum = 1;
        long written = 0;
        while (written < total) {
            Page<User> page = userMapper.selectPage(Page.of(pageNum, EXPORT_BATCH_SIZE), wrapper);
            List<User> records = page.getRecords();
            if (records.isEmpty()) {
                break;
            }
            List<UserExportVO> rows = new ArrayList<>(records.size());
            for (UserVO vo : enrichUsers(records)) {
                rows.add(UserExportVO.builder()
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
            writer.write(rows, sheet);
            written += records.size();
            pageNum++;
        }
        writer.finish();
    }

    @Override
    @Transactional(readOnly = true)
    public R<UserVO> detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(id);
        UserVO vo = enrichUsers(List.of(user)).get(0);
        return R.ok(new UserVO(
                vo.id(), vo.username(), vo.nickname(), vo.realName(),
                vo.email(), vo.phone(), vo.avatar(), vo.sex(), vo.sexLabel(),
                vo.deptId(), vo.deptName(), vo.postId(), vo.postName(),
                vo.status(), vo.statusLabel(), vo.loginIp(), vo.loginDate(),
                vo.createTime(), vo.remark(),
                roleIds
        ));
    }

    /**
     * 管理端详情。数据权限由 {@code @DataScope} + MyBatis-Plus 拦截器在 SQL 层生效，
     * 不可见的记录查询结果为空，与「用户不存在」同义。
     *
     * <p>与 {@link #detail(Long)} 分离，避免个人中心（本人）被数据范围误过滤。</p>
     */
    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_user", deptColumn = "dept_id", userColumn = "create_by")
    public R<UserVO> detailInScope(Long id) {
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(UserSaveDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.username()));
        if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);

        User user = userConvert.toEntity(dto);
        user.setPassword(BCrypt.hashpw(dto.password()));
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.insert(user);
        return R.ok(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(UserSaveDTO dto) {
        User exist = userMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        guardProtectedAccount(exist, dto);

        // 仅在提交了新登录名时才做重名校验（状态切换等局部更新不会传 username）
        if (StrUtil.isNotBlank(dto.username()) && !exist.getUsername().equals(dto.username())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.username())
                    .ne(User::getId, dto.id()));
            if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        User update = userConvert.toUpdateEntity(dto);
        userMapper.updateById(update);
        return R.ok(true);
    }

    /**
     * 内置管理员账号与当前登录用户的自我保护：
     * 既不允许停用 / 改名 admin，也不允许停用自己，避免把管理员锁在系统外。
     */
    private void guardProtectedAccount(User exist, UserSaveDTO dto) {
        boolean self = exist.getId().equals(StpUtil.getLoginIdAsLong());
        boolean admin = ADMIN_USERNAME.equals(exist.getUsername());
        if ((self || admin) && dto.status() != null && dto.status() == 0) {
            throw new BusinessException(self ? ErrorCode.USER_CANNOT_DISABLE_SELF : ErrorCode.USER_PROTECTED);
        }
        if (admin && StrUtil.isNotBlank(dto.username()) && !ADMIN_USERNAME.equals(dto.username())) {
            throw new BusinessException(ErrorCode.USER_PROTECTED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        if (ids.contains(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DELETE_SELF);
        }
        Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .in(User::getId, ids)
                .eq(User::getUsername, ADMIN_USERNAME));
        if (adminCount > 0) throw new BusinessException(ErrorCode.USER_PROTECTED);
        // 级联清理关联与在线会话，避免孤儿数据与残留权限
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, ids));
        userPostMapper.delete(new LambdaQueryWrapper<UserPost>().in(UserPost::getUserId, ids));
        userMapper.deleteBatchIds(ids);
        for (Long id : ids) {
            try {
                cn.dev33.satoken.stp.StpUtil.logout(id);
            } catch (Exception ignored) {
            }
            permissionService.clearUserPermissionCache(id);
        }
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
        permissionService.clearUserPermissionCache(userId);
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
        // 改密后强制下线所有会话，避免旧凭证继续有效
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {
        }
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
    public User getById(Long id) {
        return userMapper.selectById(id);
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

    /** 批量填充部门、岗位名称，避免逐行 selectById 产生 N+1 查询 */
    private List<UserVO> enrichUsers(List<User> users) {
        Set<Long> deptIds = users.stream()
                .map(User::getDeptId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<Long> postIds = users.stream()
                .map(User::getPostId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, Dept::getName, (a, b) -> a));
        Map<Long, String> postNameMap = postIds.isEmpty() ? Map.of()
                : postMapper.selectBatchIds(postIds).stream()
                        .collect(Collectors.toMap(Post::getId, Post::getName, (a, b) -> a));
        return users.stream().map(u -> toVOWithExtra(u, deptNameMap, postNameMap)).toList();
    }

    private UserVO toVOWithExtra(User user, Map<Long, String> deptNameMap, Map<Long, String> postNameMap) {
        UserVO vo = userConvert.toVO(user);
        String deptName = (user.getDeptId() != null && user.getDeptId() > 0)
                ? deptNameMap.get(user.getDeptId()) : null;
        String postName = (user.getPostId() != null && user.getPostId() > 0)
                ? postNameMap.get(user.getPostId()) : null;
        return new UserVO(
                vo.id(), vo.username(), vo.nickname(), vo.realName(),
                vo.email(), vo.phone(), vo.avatar(),
                vo.sex(), vo.sexLabel(),
                vo.deptId(), deptName, vo.postId(), postName,
                vo.status(), vo.statusLabel(),
                vo.loginIp(), vo.loginDate(), vo.createTime(), vo.remark(), null);
    }
}
