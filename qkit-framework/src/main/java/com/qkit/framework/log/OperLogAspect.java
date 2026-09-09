package com.qkit.framework.log;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qkit.common.api.R;
import com.qkit.common.constant.SecurityConstants;
import com.qkit.common.log.spi.OperLogRecord;
import com.qkit.common.log.spi.OperLogSink;
import com.qkit.common.util.WebUtil;
import com.qkit.framework.log.annotation.OperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面。拦截 {@code @OperLog} 注解方法，记录请求/响应/耗时。
 *
 * <p>敏感字段（password / satoken 等）自动脱敏。
 * 组装完 {@link OperLogRecord} 后交给 {@link OperLogSink} 落库；
 * 业务侧未提供实现时退化为控制台日志。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<OperLogSink> operLogSinkProvider;

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
                recordLog(pjp, operLog, result, error, cost);
            } catch (Exception ex) {
                log.warn("操作日志记录失败", ex);
            }
        }
    }

    /** 同步线程内组装记录：RequestContextHolder / StpUtil 均依赖当前请求线程，不可延后到异步 */
    void recordLog(ProceedingJoinPoint pjp, OperLog operLog, Object result, Throwable error, long cost) {
        HttpServletRequest request = currentRequest();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        OperLogSink sink = operLogSinkProvider.getIfAvailable();
        if (sink == null) {
            logToConsole(pjp, operLog, result, error, cost, methodName, request);
            return;
        }

        OperLogRecord record = new OperLogRecord(
                operLog.module(),
                operLog.name(),
                StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null,
                currentUsername(),
                WebUtil.getClientIp(request),
                request == null ? null : request.getHeader("User-Agent"),
                methodName,
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod(),
                operLog.saveParams() ? maskSensitive(toJson(pjp.getArgs())) : null,
                operLog.saveResult() && result != null ? toJson(result) : null,
                error == null ? 1 : 0,
                error == null ? null : truncate(ExceptionUtil.getMessage(error), 4096),
                cost,
                LocalDateTime.now());
        sink.record(record);
    }

    /** 未提供落库实现时的兜底输出（兼容只引入框架模块的场景） */
    private void logToConsole(ProceedingJoinPoint pjp, OperLog operLog, Object result, Throwable error,
                              long cost, String methodName, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("\n========== 操作日志 ==========\n");
        sb.append("模块：").append(operLog.module()).append(" / 操作：").append(operLog.name()).append("\n");
        sb.append("方法：").append(methodName).append("\n");
        sb.append("URL：").append(request == null ? "" : request.getRequestURI()).append("\n");
        sb.append("IP：").append(WebUtil.getClientIp(request)).append("\n");
        if (StpUtil.isLogin()) {
            sb.append("操作人：").append(StpUtil.getLoginIdAsLong()).append("\n");
        }
        sb.append("耗时：").append(cost).append(" ms\n");
        if (operLog.saveParams()) {
            sb.append("入参：").append(truncate(maskSensitive(toJson(pjp.getArgs())), 2048)).append("\n");
        }
        if (operLog.saveResult() && result != null) {
            sb.append("响应：").append(truncate(toJson(result), 2048)).append("\n");
        }
        if (error != null) {
            sb.append("异常：").append(truncate(ExceptionUtil.getMessage(error), 4096)).append("\n");
        }
        sb.append("==============================");
        log.info(sb.toString());
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 当前登录账号。登录成功时由 AuthServiceImpl 写入会话；未登录返回 null */
    private String currentUsername() {
        if (!StpUtil.isLogin()) return null;
        try {
            return StpUtil.getSession().getString(SecurityConstants.SESSION_USERNAME);
        } catch (Exception e) {
            return null;
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
}