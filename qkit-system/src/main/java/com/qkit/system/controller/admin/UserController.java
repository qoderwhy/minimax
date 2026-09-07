package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/admin-api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @SaCheckPermission("system:user:page")
    public R<List<UserVO>> page(UserQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = UserQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return userService.page(query);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/detail/{id}")
    @SaCheckPermission("system:user:detail")
    public R<UserVO> detail(@PathVariable Long id) {
        return userService.detail(id);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @SaCheckPermission("system:user:create")
    @OperLog(module = "用户管理", name = "新增用户")
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) UserSaveDTO dto) {
        return userService.create(dto);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    @SaCheckPermission("system:user:update")
    @OperLog(module = "用户管理", name = "更新用户")
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) UserSaveDTO dto) {
        return userService.update(dto);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:user:delete")
    @OperLog(module = "用户管理", name = "删除用户")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return userService.delete(ids);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    @SaCheckPermission("system:user:reset-password")
    @OperLog(module = "用户管理", name = "重置密码")
    public R<Boolean> resetPassword(@RequestParam Long userId, @RequestParam String newPassword) {
        return userService.resetPassword(userId, newPassword);
    }

    @Operation(summary = "分配角色")
    @PutMapping("/assign-role")
    @SaCheckPermission("system:user:assign-role")
    @OperLog(module = "用户管理", name = "分配角色")
    public R<Boolean> assignRole(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        return userService.assignRole(userId, roleIds);
    }

    @Operation(summary = "修改密码（本人）")
    @PutMapping("/change-password")
    public R<Boolean> changePassword(@RequestParam Long userId, @RequestBody @Valid PasswordDTO dto) {
        return userService.changePassword(userId, dto);
    }
}
