package com.qkit.system.convert;

import com.qkit.system.domain.dto.DictSaveDTO;
import com.qkit.system.domain.entity.Dict;
import com.qkit.system.domain.vo.DictVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictConvert {

    DictConvert INSTANCE = Mappers.getMapper(DictConvert.class);

    DictVO toVO(Dict entity);

    List<DictVO> toVOList(List<Dict> list);

    Dict toEntity(DictSaveDTO dto);
}
