package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.LoginDTO;
import com.qkit.system.domain.vo.CaptchaVO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.LoginVO;

public interface AuthService {

    R<CaptchaVO> captcha();

    R<LoginVO> login(LoginDTO dto, String clientIp);

    R<Void> logout();

    R<LoginUserVO> me();
}
