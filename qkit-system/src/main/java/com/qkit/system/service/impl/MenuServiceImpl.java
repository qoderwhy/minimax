package com.qkit.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.MenuConvert;
import com.qkit.system.domain.dto.MenuSaveDTO;
import com.qkit.system.domain.entity.Menu;
import com.qkit.system.domain.vo.MenuVO;
import com.qkit.system.domain.vo.RouteMetaVO;
import com.qkit.system.domain.vo.RouteVO;
import com.qkit.system.enums.MenuTypeEnum;
import com.qkit.system.mapper.MenuMapper;
import com.qkit.system.service.MenuService;
import com.qkit.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final MenuConvert menuConvert;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public R<List<MenuVO>> tree(String name) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<Menu>()
                .like(name != null && !name.isBlank(), Menu::getName, name)
                .orderByAsc(Menu::getSort);
        List<Menu> all = menuMapper.selectList(wrapper);
        Map<Long, List<Menu>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return R.ok(buildTree(0L, byParent));
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<RouteVO>> currentUserRoute() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<String> perms = permissionService.getUserPermissions(userId);
        List<String> roleCodes = permissionService.getUserRoleCodes(userId);
        // 一次性加载所有启用的目录(M)/菜单(C)，普通用户再按「有权限的菜单 + 其祖先目录」过滤
        List<Menu> all = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .in(Menu::getType, "M", "C")
                .orderByAsc(Menu::getSort));
        List<Menu> menus = (perms.contains("*:*:*") || roleCodes.contains("admin"))
                ? all
                : visibleMenus(all, new java.util.HashSet<>(perms));
        Map<Long, List<Menu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return R.ok(buildRoute(0L, byParent));
    }

    /**
     * 普通用户可见菜单：保留有权限的菜单(C)，并沿父链补齐所有祖先目录(M)，
     * 避免目录无 perm 时整棵子树对普通用户不可见。
     */
    private List<Menu> visibleMenus(List<Menu> all, java.util.Set<String> perms) {
        java.util.Map<Long, Menu> byId = all.stream()
                .collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a));
        java.util.Set<Long> visibleIds = new java.util.HashSet<>();
        for (Menu m : all) {
            if (!MenuTypeEnum.MENU.getCode().equals(m.getType())) continue;
            if (m.getPerm() == null || !perms.contains(m.getPerm())) continue;
            // 当前菜单向上追溯到根，沿途节点全部标记可见
            Long cur = m.getId();
            while (cur != null && visibleIds.add(cur)) {
                Menu node = byId.get(cur);
                if (node == null) break;
                cur = (node.getParentId() == null || node.getParentId() == 0L) ? null : node.getParentId();
            }
        }
        return all.stream().filter(m -> visibleIds.contains(m.getId())).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(MenuSaveDTO dto) {
        Menu menu = menuConvert.toEntity(dto);
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getStatus() == null) menu.setStatus(1);
        if (menu.getVisible() == null) menu.setVisible(1);
        if (menu.getKeepAlive() == null) menu.setKeepAlive(0);
        menuMapper.insert(menu);
        return R.ok(menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(MenuSaveDTO dto) {
        Menu exist = menuMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        Menu menu = menuConvert.toEntity(dto);
        menuMapper.updateById(menu);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        for (Long id : ids) {
            Long count = menuMapper.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
            if (count > 0) throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        menuMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    private List<MenuVO> buildTree(Long parentId, Map<Long, List<Menu>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(m -> menuConvert.toVO(m, buildTree(m.getId(), byParent)))
                .toList();
    }

    private List<RouteVO> buildRoute(Long parentId, Map<Long, List<Menu>> byParent) {
        List<Menu> children = byParent.getOrDefault(parentId, List.of());
        List<RouteVO> result = new ArrayList<>();
        for (Menu m : children) {
            List<RouteVO> sub = buildRoute(m.getId(), byParent);
            String component = m.getType().equals(MenuTypeEnum.DIR.getCode()) ? "Layout" : m.getComponent();
            RouteMetaVO meta = new RouteMetaVO(m.getName(), m.getIcon(), m.getVisible() != 1, Integer.valueOf(1).equals(m.getKeepAlive()), m.getPerm());
            RouteVO vo = new RouteVO(m.getId(), capitalize(m.getPath()), m.getPath(), component, null, meta, sub);
            result.add(vo);
        }
        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("/");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
