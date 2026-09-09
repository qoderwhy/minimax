package com.qkit.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.convert.DeptConvert;
import com.qkit.system.domain.dto.DeptSaveDTO;
import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.DeptSimpleVO;
import com.qkit.system.domain.vo.DeptTreeVO;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final DeptConvert deptConvert;
    private final DataScopeHelper dataScopeHelper;

    @Override
    @Transactional(readOnly = true)
    public R<List<DeptTreeVO>> tree(String name) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<Dept>()
                .like(name != null && !name.isBlank(), Dept::getName, name)
                .orderByAsc(Dept::getSort);
        List<Dept> all = deptMapper.selectList(wrapper);
        Map<Long, List<Dept>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        List<DeptTreeVO> fullTree = buildTree(0L, byParent);
        List<Long> visibleIds = dataScopeHelper.visibleDeptIds(userId);
        Set<Long> visible = visibleIds != null ? new HashSet<>(visibleIds) : null;
        List<DeptTreeVO> filtered = visible != null ? filterTree(fullTree, visible) : fullTree;
        return R.ok(filtered);
    }

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_dept", deptColumn = "id")
    public R<List<DeptSimpleVO>> simpleList() {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<Dept>()
                .eq(Dept::getStatus, 1).orderByAsc(Dept::getSort);
        List<Dept> all = deptMapper.selectList(wrapper);
        return R.ok(deptConvert.toSimpleVOList(all));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(DeptSaveDTO dto) {
        Dept dept = new Dept();
        dept.setName(dto.name());
        dept.setParentId(dto.parentId() == null ? 0L : dto.parentId());
        dept.setSort(dto.sort() == null ? 0 : dto.sort());
        dept.setLeader(dto.leader());
        dept.setPhone(dto.phone());
        dept.setEmail(dto.email());
        dept.setStatus(dto.status() == null ? 1 : dto.status());
        deptMapper.insert(dept);
        return R.ok(dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(DeptSaveDTO dto) {
        Dept dept = new Dept();
        dept.setId(dto.id());
        dept.setName(dto.name());
        dept.setParentId(dto.parentId() == null ? 0L : dto.parentId());
        dept.setSort(dto.sort());
        dept.setLeader(dto.leader());
        dept.setPhone(dto.phone());
        dept.setEmail(dto.email());
        dept.setStatus(dto.status());
        deptMapper.updateById(dept);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        for (Long id : ids) {
            // 1. 检查子部门
            List<Dept> all = deptMapper.selectList(null);
            Map<Long, List<Long>> parentToChildren = all.stream().collect(Collectors.groupingBy(
                    Dept::getParentId,
                    Collectors.mapping(Dept::getId, Collectors.toList())));
            int childCount = countChildren(id, parentToChildren);
            if (childCount > 0) throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
            // 2. 检查部门下用户
            Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
            if (userCount > 0) throw new BusinessException(ErrorCode.DEPT_HAS_USER);
        }
        deptMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    private int countChildren(Long rootId, Map<Long, List<Long>> parentToChildren) {
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        int count = 0;
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            count++;
            List<Long> kids = parentToChildren.getOrDefault(id, new ArrayList<>());
            kids.forEach(stack::push);
        }
        return count - 1; // 排除自身
    }

    private List<DeptTreeVO> filterTree(List<DeptTreeVO> nodes, Set<Long> visible) {
        return nodes.stream()
                .filter(n -> visible.contains(n.id()))
                .map(n -> DeptTreeVO.from(n.id(), n.parentId(), n.label(), filterTree(n.children(), visible)))
                .collect(Collectors.toList());
    }

    private List<DeptTreeVO> buildTree(Long parentId, Map<Long, List<Dept>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(d -> DeptTreeVO.from(d.getId(), d.getParentId(), d.getName(), buildTree(d.getId(), byParent)))
                .toList();
    }
}
