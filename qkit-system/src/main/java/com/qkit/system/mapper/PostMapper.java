package com.qkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qkit.system.domain.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
