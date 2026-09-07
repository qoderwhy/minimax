package com.qkit.system.convert;

import com.qkit.system.domain.dto.DictItemSaveDTO;
import com.qkit.system.domain.entity.DictItem;
import com.qkit.system.domain.vo.DictItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictItemConvert {

    DictItemConvert INSTANCE = Mappers.getMapper(DictItemConvert.class);

    DictItemVO toVO(DictItem entity);

    List<DictItemVO> toVOList(List<DictItem> list);

    DictItem toEntity(DictItemSaveDTO dto);
}
