package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.system.domain.vo.ServerInfoVO;
import com.qkit.system.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "服务监控")
@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "服务器信息分页")
    @GetMapping("/page")
    @SaCheckPermission("monitor:server:page")
    public R<ServerInfoVO> getServerInfo() {
        return monitorService.getServerInfo();
    }
}
