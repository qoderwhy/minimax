package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.SysConfigQueryDTO;
import com.qkit.system.domain.dto.SysConfigSaveDTO;
import com.qkit.system.domain.vo.SysConfigVO;
import com.qkit.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "参数配置管理")
@RestController
@RequestMapping("/admin-api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "分页查询参数配置")
    @GetMapping("/page")
    @SaCheckPermission("system:config:page")
    public R<List<SysConfigVO>> page(SysConfigQueryDTO query) {
        query = query.withPageDefaults();
        return sysConfigService.page(query);
    }

    @Operation(summary = "参数配置简单列表")
    @GetMapping("/list")
    @SaCheckPermission("system:config:list")
    public R<List<SysConfigVO>> list() {
        return sysConfigService.list();
    }

    @Operation(summary = "新增参数配置")
    @PostMapping("/create")
    @SaCheckPermission("system:config:create")
    @OperLog(module = "参数配置", name = "新增参数配置")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) SysConfigSaveDTO dto) {
        return sysConfigService.create(dto);
    }

    @Operation(summary = "更新参数配置")
    @PutMapping("/update")
    @SaCheckPermission("system:config:update")
    @OperLog(module = "参数配置", name = "更新参数配置")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) SysConfigSaveDTO dto) {
        return sysConfigService.update(dto);
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:config:delete")
    @OperLog(module = "参数配置", name = "删除参数配置")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return sysConfigService.delete(ids);
    }
}