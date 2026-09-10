package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.constant.SecurityConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.captcha.CaptchaUtil;
import com.qkit.framework.ratelimit.LoginRateLimiter;
import com.qkit.system.service.PermissionService;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.LoginDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.CaptchaVO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.LoginVO;
import com.qkit.system.log.LoginLogRecorder;
import com.qkit.system.service.AuthService;
import com.qkit.system.service.UserService;
import com.qkit.system.enums.DataScopeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 缓存验证码
    private final CacheService cacheService;
    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;
    private final PermissionService permissionService;
    private final LoginLogRecorder loginLogRecorder;
    private final UserConvert userConvert;

    @Override
    public R<CaptchaVO> captcha() {
        CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
        cacheService.set(
                CacheConstants.CAPTCHA_KEY_PREFIX + result.uuid(),
                result.code(),
                Duration.ofMinutes(3));
        return R.ok(new CaptchaVO(result.uuid(), result.base64()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<LoginVO> login(LoginDTO dto, String clientIp) {
        // 1. 校验失败次数
        loginRateLimiter.validate(dto.username());

        // 2. 校验图形验证码
        if (StrUtil.isNotBlank(dto.captchaId())) {
            String code = cacheService.get(CacheConstants.CAPTCHA_KEY_PREFIX + dto.captchaId());
            if (code == null || !code.equalsIgnoreCase(dto.captchaCode())) {
                loginLogRecorder.record(null, dto.username(), clientIp, 0, "验证码错误");
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
            cacheService.delete(CacheConstants.CAPTCHA_KEY_PREFIX + dto.captchaId());
        }

        // 3. 校验用户
        User user = userService.getByUsername(dto.username());
        if (user == null) {
            loginRateLimiter.onLoginFail(dto.username());
            loginLogRecorder.record(null, dto.username(), clientIp, 0, "用户不存在");
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            loginLogRecorder.record(user.getId(), dto.username(), clientIp, 0, "用户已停用");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.password(), user.getPassword())) {
            loginRateLimiter.onLoginFail(dto.username());
            loginLogRecorder.record(user.getId(), dto.username(), clientIp, 0, "密码错误");
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 4. 登录成功
        loginRateLimiter.onLoginSuccess(dto.username());
        StpUtil.login(user.getId());
        StpUtil.getSessionByLoginId(user.getId()).set(SecurityConstants.SESSION_USERNAME, user.getUsername());
        userService.updateLoginInfo(user.getId(), clientIp);
        loginLogRecorder.record(user.getId(), dto.username(), clientIp, 1, "登录成功");

        return R.ok(new LoginVO(StpUtil.getTokenValue(), user.getId(), user.getUsername(), user.getNickname()));
    }

    @Override
    public R<Void> logout(String clientIp) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            String username = StpUtil.getSessionByLoginId(userId).getString(SecurityConstants.SESSION_USERNAME);
            StpUtil.logout();
            loginLogRecorder.record(userId, username, clientIp, 1, "退出登录");
        } catch (Exception ignored) {
        }
        return R.ok();
    }

    @Override
    public R<LoginUserVO> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        LoginUserVO vo = userConvert.toLoginUserVO(user);
        return R.ok(new LoginUserVO(
                vo.userId(), vo.username(), vo.nickname(), vo.realName(), vo.avatar(),
                vo.deptId(), vo.deptName(),
                permissionService.getUserRoleCodes(userId),
                permissionService.getUserPermissions(userId)));
    }

}
