package com.qkit.framework.repeat;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.cache.CacheService;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.exception.BusinessException;
import com.qkit.common.util.WebUtil;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.framework.repeat.annotation.RepeatType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 防重复提交切面。拦截 {@code @RepeatSubmit} 注解方法。
 *
 * <p>利用 Redis {@code SET NX EX} 的原子性：首次提交写入 key（窗口期内独占），
 * 窗口期内的重复提交命中已有 key，直接抛 {@link ErrorCode#REPEAT_SUBMIT}。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        if (request == null) return pjp.proceed();

        String key = buildKey(repeatSubmit, request, pjp);
        Boolean locked = cacheService.setIfAbsent(
                key, "1", Duration.ofSeconds(repeatSubmit.interval()));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT);
        }
        return pjp.proceed();
    }

    private String buildKey(RepeatSubmit repeatSubmit, HttpServletRequest request, ProceedingJoinPoint pjp) {
        StringBuilder key = new StringBuilder(CacheConstants.REPEAT_KEY_PREFIX);
        if (repeatSubmit.type() == RepeatType.USER) {
            key.append(StpUtil.isLogin()
                    ? "user:" + StpUtil.getLoginIdAsLong()
                    : "ip:" + WebUtil.getClientIp(request));
        } else if (repeatSubmit.type() == RepeatType.IP) {
            key.append("ip:").append(WebUtil.getClientIp(request));
        } else {
            key.append("all");
        }
        key.append(':').append(request.getRequestURI());
        if (repeatSubmit.useParams()) {
            key.append(':').append(md5(pjp.getArgs()));
        }
        return key.toString();
    }

    /** 请求参数摘要（MD5 前 16 位），用于按参数区分是否重复提交 */
    private String md5(Object[] args) {
        if (args == null || args.length == 0) return "empty";
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(serialize(arg)).append('|');
        }
        return SecureUtil.md5(sb.toString()).substring(0, 16);
    }

    private String serialize(Object value) {
        if (value == null) return "null";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}