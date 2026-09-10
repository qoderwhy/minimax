package com.qkit.system.convert;

import com.qkit.system.domain.dto.MenuSaveDTO;
import com.qkit.system.domain.entity.Menu;
import com.qkit.system.domain.vo.MenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuConvert {

    MenuConvert INSTANCE = Mappers.getMapper(MenuConvert.class);

    MenuVO toVO(Menu entity, List<MenuVO> children);

    Menu toEntity(MenuSaveDTO dto);
}
