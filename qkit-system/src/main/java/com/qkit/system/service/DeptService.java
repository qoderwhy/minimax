package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.DeptSaveDTO;
import com.qkit.system.domain.vo.DeptSimpleVO;
import com.qkit.system.domain.vo.DeptTreeVO;

import java.util.List;

public interface DeptService {

    R<List<DeptTreeVO>> tree(String name);

    R<List<DeptSimpleVO>> simpleList();

    R<Long> create(DeptSaveDTO dto);

    R<Boolean> update(DeptSaveDTO dto);

    R<Boolean> delete(List<Long> ids);
}
