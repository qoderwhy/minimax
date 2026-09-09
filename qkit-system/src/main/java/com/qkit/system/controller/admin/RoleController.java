package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.RoleQueryDTO;
import com.qkit.system.domain.dto.RoleSaveDTO;
import com.qkit.system.domain.vo.RoleVO;
import com.qkit.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/admin-api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @SaCheckPermission("system:role:page")
    public R<List<RoleVO>> page(RoleQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = RoleQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return roleService.page(query);
    }

    @Operation(summary = "角色下拉列表")
    @GetMapping("/list")
    @SaCheckPermission("system:role:list")
    public R<List<RoleVO>> list() {
        return roleService.list();
    }

    @Operation(summary = "角色详情")
    @GetMapping("/detail/{id}")
    public R<RoleVO> detail(@PathVariable Long id) {
        return roleService.detail(id);
    }

    @Operation(summary = "新增角色")
    @PostMapping("/create")
    @SaCheckPermission("system:role:create")
    @OperLog(module = "角色管理", name = "新增角色")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) RoleSaveDTO dto) {
        return roleService.create(dto);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/update")
    @SaCheckPermission("system:role:update")
    @OperLog(module = "角色管理", name = "更新角色")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) RoleSaveDTO dto) {
        return roleService.update(dto);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:role:delete")
    @OperLog(module = "角色管理", name = "删除角色")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return roleService.delete(ids);
    }

    @Operation(summary = "分配菜单")
    @PutMapping("/assign-menu")
    @SaCheckPermission("system:role:assign-menu")
    @OperLog(module = "角色管理", name = "分配菜单")
    @RepeatSubmit
    public R<Boolean> assignMenu(@RequestParam Long roleId, @RequestBody List<Long> menuIds) {
        return roleService.assignMenu(roleId, menuIds);
    }

    @Operation(summary = "查询角色已分配部门")
    @GetMapping("/dept-ids")
    @SaCheckPermission("system:role:assign-dept")
    public R<List<Long>> getDeptIds(@RequestParam Long roleId) {
        return roleService.getDeptIds(roleId);
    }

    @Operation(summary = "分配部门")
    @PutMapping("/assign-dept")
    @SaCheckPermission("system:role:assign-dept")
    @OperLog(module = "角色管理", name = "分配部门")
    @RepeatSubmit
    public R<Boolean> assignDept(@RequestParam Long roleId, @RequestBody List<Long> deptIds) {
        return roleService.assignDept(roleId, deptIds);
    }
}
