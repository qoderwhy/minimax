package com.qkit.system.service.impl;

import com.qkit.common.cache.CacheService;
import com.qkit.system.domain.entity.Menu;
import com.qkit.system.domain.entity.Role;
import com.qkit.system.domain.entity.RoleMenu;
import com.qkit.system.domain.entity.UserRole;
import com.qkit.system.enums.DataScopeEnum;
import com.qkit.system.mapper.MenuMapper;
import com.qkit.system.mapper.RoleMapper;
import com.qkit.system.mapper.RoleMenuMapper;
import com.qkit.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限码计算与缓存失效单元测试。
 *
 * <p>其中「清除缓存的 key 必须与鉴权读取的 key 一致」是关键回归点：
 * 一旦两侧 key 不一致，权限撤销将不会即时生效。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("权限计算与缓存失效")
class PermissionServiceImplTest {

    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    @DisplayName("admin 角色直接获得全量通配权限")
    void adminRoleGetsAllPermission() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(1L)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role("admin", DataScopeEnum.ALL)));

        assertThat(permissionService.getUserPermissions(1L)).containsExactly("*:*:*");
    }

    @Test
    @DisplayName("普通角色通过「角色-菜单」得到权限码")
    void normalRoleResolvesPermissionsFromMenus() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(2L)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role("common", DataScopeEnum.SELF)));
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(roleMenu(100L)));
        when(menuMapper.selectList(any())).thenReturn(List.of(menu("system:user:page")));

        assertThat(permissionService.getUserPermissions(1L)).containsExactly("system:user:page");
    }

    @Test
    @DisplayName("未分配角色时权限为空")
    void noRoleMeansNoPermission() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        assertThat(permissionService.getUserPermissions(1L)).isEmpty();
        assertThat(permissionService.getUserRoleCodes(1L)).isEmpty();
    }

    @Test
    @DisplayName("数据范围取最宽松的一档")
    void dataScopePicksBroadestScope() {
        when(userRoleMapper.selectList(any()))
                .thenReturn(List.of(userRole(2L), userRole(3L)));
        when(roleMapper.selectBatchIds(any()))
                .thenReturn(List.of(role("a", DataScopeEnum.SELF), role("b", DataScopeEnum.DEPT_AND_CHILD)));

        assertThat(permissionService.getDataScope(1L)).isEqualTo(DataScopeEnum.DEPT_AND_CHILD);
    }

    @Test
    @DisplayName("清除缓存时删除鉴权实际读取的 perm:{userId} 键")
    void clearCacheDeletesPermissionKeyUsedByStpInterface() {
        permissionService.clearUserPermissionCache(7L);

        verify(cacheService).delete("perm:7");
    }

    private UserRole userRole(Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(1L);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private Role role(String code, DataScopeEnum scope) {
        Role role = new Role();
        role.setCode(code);
        role.setDataScope(scope.getCode());
        return role;
    }

    private RoleMenu roleMenu(Long menuId) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(1L);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private Menu menu(String perm) {
        Menu menu = new Menu();
        menu.setPerm(perm);
        return menu;
    }
}
