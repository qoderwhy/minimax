package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.dto.UserResetPasswordDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) UserSaveDTO dto) {
        return userService.create(dto);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    @SaCheckPermission("system:user:update")
    @OperLog(module = "用户管理", name = "更新用户")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) UserSaveDTO dto) {
        return userService.update(dto);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:user:delete")
    @OperLog(module = "用户管理", name = "删除用户")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return userService.delete(ids);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    @SaCheckPermission("system:user:reset-password")
    @OperLog(module = "用户管理", name = "重置密码")
    @RepeatSubmit
    public R<Boolean> resetPassword(@RequestBody @Valid UserResetPasswordDTO dto) {
        return userService.resetPassword(dto.userId(), dto.newPassword());
    }

    @Operation(summary = "分配角色")
    @PutMapping("/assign-role")
    @SaCheckPermission("system:user:assign-role")
    @OperLog(module = "用户管理", name = "分配角色")
    @RepeatSubmit
    public R<Boolean> assignRole(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        return userService.assignRole(userId, roleIds);
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    @SaCheckPermission("system:user:export")
    @OperLog(module = "用户管理", name = "导出用户")
    public void export(UserQueryDTO query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        userService.export(query, response);
    }

    // ==================== 个人中心（本人） ====================

    @Operation(summary = "个人资料详情")
    @GetMapping("/profile")
    public R<UserVO> profile() {
        return userService.profile(StpUtil.getLoginIdAsLong());
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    @OperLog(module = "个人中心", name = "更新个人资料")
    @RepeatSubmit
    public R<Boolean> updateProfile(@RequestBody @Valid UserProfileUpdateDTO dto) {
        return userService.updateProfile(StpUtil.getLoginIdAsLong(), dto);
    }

    @Operation(summary = "修改密码（本人）")
    @PutMapping("/profile/password")
    @OperLog(module = "个人中心", name = "修改密码")
    @RepeatSubmit
    public R<Boolean> changePassword(@RequestBody @Valid PasswordDTO dto) {
        return userService.changePassword(StpUtil.getLoginIdAsLong(), dto);
    }
}
