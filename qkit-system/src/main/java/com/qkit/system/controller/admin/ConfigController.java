package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.ConfigQueryDTO;
import com.qkit.system.domain.dto.ConfigSaveDTO;
import com.qkit.system.domain.vo.ConfigVO;
import com.qkit.system.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "参数配置管理")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "分页查询参数配置")
    @GetMapping("/page")
    @SaCheckPermission("system:config:page")
    public R<List<ConfigVO>> page(ConfigQueryDTO query) {
        query = query.withPageDefaults();
        return configService.page(query);
    }

    @Operation(summary = "参数配置简单列表")
    @GetMapping("/list")
    @SaCheckPermission("system:config:list")
    public R<List<ConfigVO>> list() {
        return configService.list();
    }

    @Operation(summary = "新增参数配置")
    @PostMapping("/create")
    @SaCheckPermission("system:config:create")
    @OperLog(module = "参数配置", name = "新增参数配置")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) ConfigSaveDTO dto) {
        return configService.create(dto);
    }

    @Operation(summary = "更新参数配置")
    @PutMapping("/update")
    @SaCheckPermission("system:config:update")
    @OperLog(module = "参数配置", name = "更新参数配置")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) ConfigSaveDTO dto) {
        return configService.update(dto);
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:config:delete")
    @OperLog(module = "参数配置", name = "删除参数配置")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return configService.delete(ids);
    }
}
