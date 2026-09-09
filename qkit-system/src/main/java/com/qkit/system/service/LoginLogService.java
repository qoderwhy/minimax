package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.LoginLogQueryDTO;
import com.qkit.system.domain.vo.LoginLogVO;

import java.util.List;

public interface LoginLogService {
    R<List<LoginLogVO>> page(LoginLogQueryDTO query);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> clean();
}
