package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.DictItemQueryDTO;
import com.qkit.system.domain.dto.DictItemSaveDTO;
import com.qkit.system.domain.dto.DictQueryDTO;
import com.qkit.system.domain.dto.DictSaveDTO;
import com.qkit.system.domain.vo.DictItemVO;
import com.qkit.system.domain.vo.DictVO;

import java.util.List;

public interface DictService {

    R<List<DictVO>> page(DictQueryDTO query);

    R<List<DictItemVO>> listItems(String dictType);

    /** 字典项分页查询（管理端，包含停用项） */
    R<List<DictItemVO>> pageItems(DictItemQueryDTO query);

    R<Long> createDict(DictSaveDTO dto);

    R<Boolean> updateDict(DictSaveDTO dto);

    R<Boolean> deleteDict(List<Long> ids);

    R<Long> createItem(DictItemSaveDTO dto);

    R<Boolean> updateItem(DictItemSaveDTO dto);

    R<Boolean> deleteItem(List<Long> ids);

    /** 加载字典到 Redis（启动时调用） */
    void loadAllToCache();

    /** 根据 type 获取字典项（优先走 Redis） */
    List<DictItemVO> getItemsByType(String type);
}
