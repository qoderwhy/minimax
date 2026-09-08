package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.captcha.CaptchaUtil;
import com.qkit.framework.ratelimit.LoginRateLimiter;
import com.qkit.system.service.PermissionService;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.LoginDTO;
import com.qkit.system.domain.entity.LoginLog;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.CaptchaVO;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.LoginVO;
import com.qkit.system.mapper.LoginLogMapper;
import com.qkit.system.service.AuthService;
import com.qkit.system.service.UserService;
import com.qkit.system.enums.DataScopeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 缓存验证码
    private final CacheService cacheService;
    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;
    private final PermissionService permissionService;
    private final LoginLogMapper loginLogMapper;
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
                recordLoginLog(null, dto.username(), clientIp, 1, "验证码错误");
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
            cacheService.delete(CacheConstants.CAPTCHA_KEY_PREFIX + dto.captchaId());
        }

        // 3. 校验用户
        User user = userService.getByUsername(dto.username());
        if (user == null) {
            loginRateLimiter.onLoginFail(dto.username());
            recordLoginLog(null, dto.username(), clientIp, 1, "用户不存在");
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            recordLoginLog(user.getId(), dto.username(), clientIp, 1, "用户已停用");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(dto.password(), user.getPassword())) {
            loginRateLimiter.onLoginFail(dto.username());
            recordLoginLog(user.getId(), dto.username(), clientIp, 1, "密码错误");
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 4. 登录成功
        loginRateLimiter.onLoginSuccess(dto.username());
        StpUtil.login(user.getId());
        StpUtil.getSessionByLoginId(user.getId()).set("username", user.getUsername());
        userService.updateLoginInfo(user.getId(), clientIp);
        recordLoginLog(user.getId(), dto.username(), clientIp, 0, "登录成功");

        return R.ok(new LoginVO(StpUtil.getTokenValue(), user.getId(), user.getUsername(), user.getNickname()));
    }

    @Override
    public R<Void> logout() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
        return R.ok();
    }

    @Override
    public R<LoginUserVO> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getByUsername(currentUsername());
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        LoginUserVO vo = userConvert.toLoginUserVO(user);
        return R.ok(new LoginUserVO(
                vo.userId(), vo.username(), vo.nickname(), vo.realName(), vo.avatar(),
                vo.deptId(), vo.deptName(),
                permissionService.getUserRoleCodes(userId),
                permissionService.getUserPermissions(userId)));
    }

    private String currentUsername() {
        // 通过缓存或查询
        return StpUtil.getSessionByLoginId(StpUtil.getLoginIdAsLong()).getString("username");
    }

    private void recordLoginLog(Long userId, String username, String ip, int status, String message) {
        try {
            LoginLog log = new LoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setIp(ip);
            log.setStatus(status);
            log.setMessage(message);
            log.setLoginTime(LocalDateTime.now());
            HttpServletRequest req = currentRequest();
            if (req != null) {
                log.setUserAgent(req.getHeader("User-Agent"));
            }
            loginLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("记录登录日志失败", e);
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
