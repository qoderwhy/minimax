package com.qkit.system.convert;

import com.qkit.system.domain.dto.PostSaveDTO;
import com.qkit.system.domain.entity.Post;
import com.qkit.system.domain.vo.PostVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostConvert {

    PostConvert INSTANCE = Mappers.getMapper(PostConvert.class);

    PostVO toVO(Post entity);

    List<PostVO> toVOList(List<Post> list);

    Post toEntity(PostSaveDTO dto);
}
