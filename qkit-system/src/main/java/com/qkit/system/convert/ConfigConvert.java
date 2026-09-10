package com.qkit.system.convert;

import com.qkit.system.domain.dto.ConfigSaveDTO;
import com.qkit.system.domain.entity.Config;
import com.qkit.system.domain.vo.ConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConfigConvert {

    ConfigVO toVO(Config entity);

    List<ConfigVO> toVOList(List<Config> list);

    Config toEntity(ConfigSaveDTO dto);
}
