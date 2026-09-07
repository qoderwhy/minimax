package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.MenuSaveDTO;
import com.qkit.system.domain.vo.MenuVO;
import com.qkit.system.domain.vo.RouteVO;

import java.util.List;

public interface MenuService {

    R<List<MenuVO>> tree(String name);

    R<List<RouteVO>> currentUserRoute();

    R<Long> create(MenuSaveDTO dto);

    R<Boolean> update(MenuSaveDTO dto);

    R<Boolean> delete(List<Long> ids);
}
