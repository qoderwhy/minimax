package com.qkit.system.convert;

import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.vo.DeptSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeptConvert {

    DeptConvert INSTANCE = Mappers.getMapper(DeptConvert.class);

    List<DeptSimpleVO> toSimpleVOList(List<Dept> list);

    DeptSimpleVO toSimpleVO(Dept entity);
}
