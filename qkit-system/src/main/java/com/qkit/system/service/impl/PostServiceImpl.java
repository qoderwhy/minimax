package com.qkit.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.PostConvert;
import com.qkit.system.domain.dto.PostQueryDTO;
import com.qkit.system.domain.dto.PostSaveDTO;
import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.entity.Post;
import com.qkit.system.domain.entity.UserPost;
import com.qkit.system.domain.vo.PostVO;
import com.qkit.system.mapper.DeptMapper;
import com.qkit.system.mapper.PostMapper;
import com.qkit.system.mapper.UserPostMapper;
import com.qkit.system.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final DeptMapper deptMapper;
    private final UserPostMapper userPostMapper;
    private final PostConvert postConvert;

    @Override
    @Transactional(readOnly = true)
    public R<List<PostVO>> page(PostQueryDTO query) {
        Page<Post> page = Page.of(
                query.pageNum() == null ? 1 : query.pageNum(),
                query.pageSize() == null ? 10 : query.pageSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .like(StrUtil.isNotBlank(query.code()), Post::getCode, query.code())
                .like(StrUtil.isNotBlank(query.name()), Post::getName, query.name())
                .eq(query.deptId() != null, Post::getDeptId, query.deptId())
                .eq(query.status() != null, Post::getStatus, query.status())
                .orderByAsc(Post::getSort);
        Page<Post> result = postMapper.selectPage(page, wrapper);
        List<PostVO> voList = toVOList(result.getRecords());
        return R.ok(voList, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<PostVO>> list() {
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1).orderByAsc(Post::getSort));
        return R.ok(toVOList(posts));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(PostSaveDTO dto) {
        Post post = postConvert.toEntity(dto);
        if (post.getStatus() == null) post.setStatus(1);
        if (post.getSort() == null) post.setSort(0);
        postMapper.insert(post);
        return R.ok(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(PostSaveDTO dto) {
        Post post = postConvert.toEntity(dto);
        postMapper.updateById(post);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        Long usedCount = userPostMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserPost>()
                        .in(UserPost::getPostId, ids));
        if (usedCount > 0) throw new BusinessException(ErrorCode.POST_IN_USE);
        postMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    /** 批量填充部门名称，避免逐行 selectById 产生 N+1 查询 */
    private List<PostVO> toVOList(List<Post> posts) {
        Set<Long> deptIds = posts.stream()
                .map(Post::getDeptId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, Dept::getName, (a, b) -> a));
        return posts.stream().map(post -> toVOWithDept(post, deptNameMap)).toList();
    }

    private PostVO toVOWithDept(Post post, Map<Long, String> deptNameMap) {
        PostVO vo = postConvert.toVO(post);
        String deptName = (post.getDeptId() != null && post.getDeptId() > 0)
                ? deptNameMap.get(post.getDeptId()) : null;
        return new PostVO(vo.id(), vo.code(), vo.name(), vo.deptId(), deptName,
                vo.sort(), vo.status(), vo.remark(), vo.createTime());
    }
}
