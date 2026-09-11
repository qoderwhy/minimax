package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.vo.ServerInfoVO;

public interface MonitorService {
    R<ServerInfoVO> getServerInfo();
}
