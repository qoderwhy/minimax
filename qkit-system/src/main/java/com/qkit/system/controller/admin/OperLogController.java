package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.OperLogQueryDTO;
import com.qkit.system.domain.vo.OperLogVO;
import com.qkit.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/system/oper-log")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @SaCheckPermission("system:oper-log:page")
    public R<List<OperLogVO>> page(OperLogQueryDTO query) {
        query = query.withPageDefaults();
        return operLogService.page(query);
    }

    @Operation(summary = "删除操作日志")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:oper-log:delete")
    @OperLog(module = "操作日志", name = "删除操作日志")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return operLogService.delete(ids);
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/clean")
    @SaCheckPermission("system:oper-log:clean")
    @OperLog(module = "操作日志", name = "清空操作日志")
    public R<Boolean> clean() {
        return operLogService.clean();
    }
}
