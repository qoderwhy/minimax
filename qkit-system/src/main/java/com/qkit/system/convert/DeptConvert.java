package com.qkit.system.convert;

import com.qkit.system.domain.entity.Dept;
import com.qkit.system.domain.vo.DeptSimpleVO;
import com.qkit.system.domain.vo.DeptTreeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeptConvert {

    DeptConvert INSTANCE = Mappers.getMapper(DeptConvert.class);

    @Mapping(target = "label", source = "name")
    DeptTreeVO toTreeVO(Dept entity);

    List<DeptSimpleVO> toSimpleVOList(List<Dept> list);

    DeptSimpleVO toSimpleVO(Dept entity);
}
