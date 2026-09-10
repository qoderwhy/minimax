package com.qkit.system.convert;

import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserVO toVO(User entity);

    List<UserVO> toVOList(List<User> list);

    User toEntity(UserSaveDTO dto);

    @Mapping(target = "password", ignore = true)
    User toUpdateEntity(UserSaveDTO dto);

    LoginUserVO toLoginUserVO(User user);
}
