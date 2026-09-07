package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.system.domain.dto.DictItemSaveDTO;
import com.qkit.system.domain.dto.DictQueryDTO;
import com.qkit.system.domain.dto.DictSaveDTO;
import com.qkit.system.domain.vo.DictItemVO;
import com.qkit.system.domain.vo.DictVO;
import com.qkit.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "字典管理")
@RestController
@RequestMapping("/admin-api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @Operation(summary = "分页查询字典")
    @GetMapping("/page")
    @SaCheckPermission("system:dict:page")
    public R<List<DictVO>> page(DictQueryDTO query) {
        if (query.pageNum() == null || query.pageSize() == null) {
            query = DictQueryDTO.of(
                    query.pageNum() == null ? 1L : query.pageNum(),
                    query.pageSize() == null ? 10L : query.pageSize());
        }
        return dictService.page(query);
    }

    @Operation(summary = "字典项列表")
    @GetMapping("/items")
    @SaCheckPermission("system:dict:list")
    public R<List<DictItemVO>> items(@RequestParam String type) {
        return dictService.listItems(type);
    }

    @Operation(summary = "字典项（公开，按 typeCode）")
    @GetMapping("/type/{typeCode}")
    @SaIgnore
    public R<List<DictItemVO>> typeItems(@PathVariable String typeCode) {
        return R.ok(dictService.getItemsByType(typeCode));
    }

    @Operation(summary = "新增字典")
    @PostMapping("/create")
    @SaCheckPermission("system:dict:create")
    @OperLog(module = "字典管理", name = "新增字典")
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) DictSaveDTO dto) {
        return dictService.createDict(dto);
    }

    @Operation(summary = "更新字典")
    @PutMapping("/update")
    @SaCheckPermission("system:dict:update")
    @OperLog(module = "字典管理", name = "更新字典")
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) DictSaveDTO dto) {
        return dictService.updateDict(dto);
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:dict:delete")
    @OperLog(module = "字典管理", name = "删除字典")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return dictService.deleteDict(ids);
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/item/create")
    @OperLog(module = "字典管理", name = "新增字典项")
    public R<Long> createItem(@RequestBody @Validated(SaveGroup.class) DictItemSaveDTO dto) {
        return dictService.createItem(dto);
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/item/update")
    @OperLog(module = "字典管理", name = "更新字典项")
    public R<Boolean> updateItem(@RequestBody @Validated(UpdateGroup.class) DictItemSaveDTO dto) {
        return dictService.updateItem(dto);
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/item/delete")
    @OperLog(module = "字典管理", name = "删除字典项")
    public R<Boolean> deleteItem(@RequestBody List<Long> ids) {
        return dictService.deleteItem(ids);
    }
}
