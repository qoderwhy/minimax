package com.qkit.system.convert;

import com.qkit.system.domain.dto.RoleSaveDTO;
import com.qkit.system.domain.entity.Role;
import com.qkit.system.domain.vo.RoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleConvert {

    RoleConvert INSTANCE = Mappers.getMapper(RoleConvert.class);

    RoleVO toVO(Role entity);

    List<RoleVO> toVOList(List<Role> list);

    Role toEntity(RoleSaveDTO dto);
}
