package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.OperLogQueryDTO;
import com.qkit.system.domain.vo.OperLogVO;

import java.util.List;

public interface OperLogService {
    R<List<OperLogVO>> page(OperLogQueryDTO query);

    /** 由 OperLogAspect 调用：异步写入数据库 */
    void asyncSave(com.qkit.system.domain.entity.OperLog entity);

    R<Boolean> delete(List<Long> ids);
}
