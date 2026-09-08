package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.PostQueryDTO;
import com.qkit.system.domain.dto.PostSaveDTO;
import com.qkit.system.domain.vo.PostVO;
import com.qkit.system.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "岗位管理")
@RestController
@RequestMapping("/admin-api/system/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "分页查询岗位")
    @GetMapping("/page")
    @SaCheckPermission("system:post:page")
    public R<List<PostVO>> page(PostQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = PostQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return postService.page(query);
    }

    @Operation(summary = "岗位下拉")
    @GetMapping("/list")
    @SaCheckPermission("system:post:list")
    public R<List<PostVO>> list() {
        return postService.list();
    }

    @Operation(summary = "新增岗位")
    @PostMapping("/create")
    @SaCheckPermission("system:post:create")
    @OperLog(module = "岗位管理", name = "新增岗位")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) PostSaveDTO dto) {
        return postService.create(dto);
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/update")
    @SaCheckPermission("system:post:update")
    @OperLog(module = "岗位管理", name = "更新岗位")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) PostSaveDTO dto) {
        return postService.update(dto);
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:post:delete")
    @OperLog(module = "岗位管理", name = "删除岗位")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return postService.delete(ids);
    }
}
