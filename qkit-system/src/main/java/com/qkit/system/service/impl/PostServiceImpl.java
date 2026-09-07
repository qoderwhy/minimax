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
        List<PostVO> voList = result.getRecords().stream().map(this::toVOWithDept).toList();
        return R.ok(voList, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<PostVO>> list() {
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 0).orderByAsc(Post::getSort));
        return R.ok(posts.stream().map(this::toVOWithDept).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(PostSaveDTO dto) {
        Post post = postConvert.toEntity(dto);
        if (post.getStatus() == null) post.setStatus(0);
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

    private PostVO toVOWithDept(Post post) {
        PostVO vo = postConvert.toVO(post);
        String deptName = null;
        if (post.getDeptId() != null && post.getDeptId() > 0) {
            Dept dept = deptMapper.selectById(post.getDeptId());
            if (dept != null) deptName = dept.getName();
        }
        return new PostVO(vo.id(), vo.code(), vo.name(), vo.deptId(), deptName,
                vo.sort(), vo.status(), vo.remark(), vo.createTime());
    }
}
