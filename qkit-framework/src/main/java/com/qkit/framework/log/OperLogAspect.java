package com.qkit.framework.log;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.api.R;
import com.qkit.framework.log.annotation.OperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * 操作日志切面。拦截 {@code @OperLog} 注解方法，记录请求/响应/耗时。
 *
 * <p>敏感字段（password / satoken 等）自动脱敏。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                long cost = System.currentTimeMillis() - start;
                saveLog(pjp, operLog, result, error, cost);
            } catch (Exception ex) {
                log.warn("操作日志记录失败", ex);
            }
        }
    }

    @Async
    void saveLog(ProceedingJoinPoint pjp, OperLog operLog, Object result, Throwable error, long cost) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs == null ? null : attrs.getRequest();

            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

            // 实际项目应注入 OperLogService 并写入 sys_oper_log；此处仅记录到日志
            StringBuilder sb = new StringBuilder("\n========== 操作日志 ==========\n");
            sb.append("模块：").append(operLog.module()).append(" / 操作：").append(operLog.name()).append("\n");
            sb.append("方法：").append(methodName).append("\n");
            sb.append("URL：").append(request == null ? "" : request.getRequestURI()).append("\n");
            sb.append("IP：").append(request == null ? "" : resolveClientIp(request)).append("\n");
            if (StpUtil.isLogin()) {
                sb.append("操作人：").append(StpUtil.getLoginIdAsLong()).append("\n");
            }
            sb.append("耗时：").append(cost).append(" ms\n");

            if (operLog.saveParams()) {
                String params = maskSensitive(toJson(pjp.getArgs()));
                sb.append("入参：").append(truncate(params, 2048)).append("\n");
            }
            if (operLog.saveResult() && result != null) {
                String resp = result instanceof R<?> r ? toJson(r) : toJson(result);
                sb.append("响应：").append(truncate(resp, 2048)).append("\n");
            }
            if (error != null) {
                sb.append("异常：").append(truncate(ExceptionUtil.getMessage(error), 4096)).append("\n");
            }
            sb.append("==============================");
            log.info(sb.toString());
        } catch (Exception e) {
            log.warn("操作日志记录失败", e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private String maskSensitive(String json) {
        if (json == null) return null;
        return json
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3")
                .replaceAll("(?i)(\"satoken\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3")
                .replaceAll("(?i)(\"oldPassword\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3")
                .replaceAll("(?i)(\"newPassword\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3");
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "...(truncated)" : s;
    }

    /**
     * 解析客户端真实 IP（按常见反代头顺序：X-Forwarded-For → X-Real-IP → remoteAddr）。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > -1 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
