package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.system.domain.dto.DeptSaveDTO;
import com.qkit.system.domain.vo.DeptSimpleVO;
import com.qkit.system.domain.vo.DeptTreeVO;
import com.qkit.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/admin-api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    @SaCheckPermission("system:dept:tree")
    public R<List<DeptTreeVO>> tree(@RequestParam(required = false) String name) {
        return deptService.tree(name);
    }

    @Operation(summary = "部门下拉")
    @GetMapping("/simple-list")
    @SaCheckPermission("system:dept:simple-list")
    public R<List<DeptSimpleVO>> simpleList() {
        return deptService.simpleList();
    }

    @Operation(summary = "新增部门")
    @PostMapping("/create")
    @SaCheckPermission("system:dept:create")
    @OperLog(module = "部门管理", name = "新增部门")
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) DeptSaveDTO dto) {
        return deptService.create(dto);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/update")
    @SaCheckPermission("system:dept:update")
    @OperLog(module = "部门管理", name = "更新部门")
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) DeptSaveDTO dto) {
        return deptService.update(dto);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:dept:delete")
    @OperLog(module = "部门管理", name = "删除部门")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return deptService.delete(ids);
    }
}
