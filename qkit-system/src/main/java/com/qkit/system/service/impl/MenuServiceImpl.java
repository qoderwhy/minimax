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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<Menu> all = menuMapper.selectList(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        List<Menu> nodes = (name == null || name.isBlank()) ? all : withAncestors(all, name);
        Map<Long, List<Menu>> byParent = nodes.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return R.ok(buildTree(0L, byParent));
    }

    /**
     * 关键字过滤：保留命中节点及其全部祖先。
     * 不在 SQL 上直接 like，避免父节点未命中时整棵子树从结果中消失。
     */
    private List<Menu> withAncestors(List<Menu> all, String keyword) {
        Map<Long, Menu> byId = all.stream().collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a));
        Set<Long> keep = new HashSet<>();
        for (Menu menu : all) {
            if (menu.getName() == null || !menu.getName().contains(keyword)) continue;
            Long cur = menu.getId();
            while (cur != null && keep.add(cur)) {
                Menu node = byId.get(cur);
                if (node == null) break;
                cur = (node.getParentId() == null || node.getParentId() == 0L) ? null : node.getParentId();
            }
        }
        return all.stream().filter(m -> keep.contains(m.getId())).toList();
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
                : visibleMenus(all, new HashSet<>(perms));
        Map<Long, List<Menu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return R.ok(buildRoute(0L, byParent));
    }

    /**
     * 普通用户可见菜单：保留有权限的菜单(C)，并沿父链补齐所有祖先目录(M)，
     * 避免目录无 perm 时整棵子树对普通用户不可见。
     */
    private List<Menu> visibleMenus(List<Menu> all, Set<String> perms) {
        Map<Long, Menu> byId = all.stream()
                .collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a));
        Set<Long> visibleIds = new HashSet<>();
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
        validateParent(dto.id(), dto.parentId());
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

    /**
     * 校验上级节点合法：沿目标父节点向上回溯，若命中自身说明目标父节点位于自己的子树中（含自环），
     * 会让该节点从根不可达。
     */
    private void validateParent(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) return;
        Map<Long, Long> parentOf = menuMapper.selectList(null).stream()
                .collect(Collectors.toMap(Menu::getId,
                        m -> m.getParentId() == null ? 0L : m.getParentId(), (a, b) -> a));
        Long cur = parentId;
        while (cur != null && cur != 0L) {
            if (cur.equals(id)) throw new BusinessException(ErrorCode.MENU_PARENT_INVALID);
            cur = parentOf.get(cur);
        }
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
            // 路由名固定为 menu-{id}：由路径拼凑的名字可能重复，会导致登出时按名移除路由误删
            RouteVO vo = new RouteVO(m.getId(), "menu-" + m.getId(), m.getPath(), component, null, meta, sub);
            result.add(vo);
        }
        return result;
    }
}
