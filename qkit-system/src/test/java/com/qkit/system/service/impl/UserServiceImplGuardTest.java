package com.qkit.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.PostMapper;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.mapper.UserPostMapper;
import com.qkit.system.mapper.UserRoleMapper;
import com.qkit.system.service.PermissionService;
import com.qkit.system.service.UserRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户自锁保护单元测试：禁止删除/停用自己与内置管理员账号。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户自锁保护")
class UserServiceImplGuardTest {

    private static final long ADMIN_ID = 1L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private UserPostMapper userPostMapper;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private UserConvert userConvert;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("不能删除当前登录用户")
    void cannotDeleteSelf() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(ADMIN_ID);

            assertThatThrownBy(() -> userService.delete(List.of(ADMIN_ID)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.USER_CANNOT_DELETE_SELF.getMessage());

            verify(userMapper, never()).deleteBatchIds(any());
        }
    }

    @Test
    @DisplayName("不能删除内置管理员账号")
    void cannotDeleteAdminAccount() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);

            assertThatThrownBy(() -> userService.delete(List.of(ADMIN_ID)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.USER_PROTECTED.getMessage());

            verify(userMapper, never()).deleteBatchIds(any());
        }
    }

    @Test
    @DisplayName("不能停用当前登录用户")
    void cannotDisableSelf() {
        when(userMapper.selectById(ADMIN_ID)).thenReturn(user(ADMIN_ID, "admin"));
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(ADMIN_ID);

            assertThatThrownBy(() -> userService.update(saveDto(ADMIN_ID, "admin", 0)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.USER_CANNOT_DISABLE_SELF.getMessage());

            verify(userMapper, never()).updateById(any(User.class));
        }
    }

    @Test
    @DisplayName("不能重命名内置管理员账号")
    void cannotRenameAdminAccount() {
        when(userMapper.selectById(ADMIN_ID)).thenReturn(user(ADMIN_ID, "admin"));
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);

            assertThatThrownBy(() -> userService.update(saveDto(ADMIN_ID, "admin2", 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.USER_PROTECTED.getMessage());

            verify(userMapper, never()).updateById(any(User.class));
        }
    }

    @Test
    @DisplayName("普通用户可正常停用")
    void canDisableNormalUser() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester"));
        when(userConvert.toUpdateEntity(any())).thenReturn(new User());
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);

            assertThat(userService.update(saveDto(2L, "tester", 0)).getData()).isTrue();
            verify(userMapper).updateById(any(User.class));
        }
    }

    private UserSaveDTO saveDto(Long id, String username, Integer status) {
        return new UserSaveDTO(id, username, null, "昵称", null, null, null, null, null, null, status, null);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
