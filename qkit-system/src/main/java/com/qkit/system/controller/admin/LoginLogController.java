package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.LoginLogQueryDTO;
import com.qkit.system.domain.vo.LoginLogVO;
import com.qkit.system.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "登录日志")
@RestController
@RequestMapping("/admin-api/system/login-log")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    @SaCheckPermission("system:login-log:page")
    public R<List<LoginLogVO>> page(LoginLogQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = LoginLogQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return loginLogService.page(query);
    }

    @Operation(summary = "删除登录日志")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:login-log:delete")
    @OperLog(module = "登录日志", name = "删除登录日志")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return loginLogService.delete(ids);
    }

    @Operation(summary = "清空登录日志")
    @DeleteMapping("/clean")
    @SaCheckPermission("system:login-log:clean")
    @OperLog(module = "登录日志", name = "清空登录日志")
    public R<Boolean> clean() {
        return loginLogService.clean();
    }
}
