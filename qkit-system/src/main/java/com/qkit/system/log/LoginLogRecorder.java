package com.qkit.system.log;

import com.qkit.system.domain.entity.LoginLog;
import com.qkit.system.mapper.LoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 登录日志记录器：使用独立事务（REQUIRES_NEW）落库，
 * 避免登录失败时外层事务回滚把失败日志一并回滚。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogRecorder {

    private final LoginLogMapper loginLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String username, String ip, int status, String message) {
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