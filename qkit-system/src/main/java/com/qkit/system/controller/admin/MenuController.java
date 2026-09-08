package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.MenuSaveDTO;
import com.qkit.system.domain.vo.MenuVO;
import com.qkit.system.domain.vo.RouteVO;
import com.qkit.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/admin-api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单树")
    @GetMapping("/tree")
    @SaCheckPermission("system:menu:tree")
    public R<List<MenuVO>> tree(@RequestParam(required = false) String name) {
        return menuService.tree(name);
    }

    @Operation(summary = "当前用户路由")
    @GetMapping("/route")
    public R<List<RouteVO>> route() {
        return menuService.currentUserRoute();
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    @SaCheckPermission("system:menu:create")
    @OperLog(module = "菜单管理", name = "新增菜单")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) MenuSaveDTO dto) {
        return menuService.create(dto);
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/update")
    @SaCheckPermission("system:menu:update")
    @OperLog(module = "菜单管理", name = "更新菜单")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) MenuSaveDTO dto) {
        return menuService.update(dto);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:menu:delete")
    @OperLog(module = "菜单管理", name = "删除菜单")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return menuService.delete(ids);
    }
}
