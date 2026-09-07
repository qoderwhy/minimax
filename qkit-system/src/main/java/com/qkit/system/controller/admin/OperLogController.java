package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.framework.log.annotation.OperLog;
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
@RequestMapping("/admin-api/system/oper-log")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @SaCheckPermission("system:oper-log:page")
    public R<List<OperLogVO>> page(OperLogQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = OperLogQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return operLogService.page(query);
    }

    @Operation(summary = "删除操作日志")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:oper-log:delete")
    @OperLog(module = "操作日志", name = "删除操作日志")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return operLogService.delete(ids);
    }
}
