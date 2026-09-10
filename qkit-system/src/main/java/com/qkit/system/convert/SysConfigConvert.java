package com.qkit.system.convert;

import com.qkit.system.domain.dto.SysConfigSaveDTO;
import com.qkit.system.domain.entity.SysConfig;
import com.qkit.system.domain.vo.SysConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysConfigConvert {

    SysConfigVO toVO(SysConfig entity);

    List<SysConfigVO> toVOList(List<SysConfig> list);

    SysConfig toEntity(SysConfigSaveDTO dto);
}