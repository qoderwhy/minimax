package com.qkit.framework.ratelimit;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.constant.CacheConstants;
import com.qkit.common.constant.SecurityConstants;
import com.qkit.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录限流：按「用户名 + 来源 IP」双维度计数，超限分别返回账号锁定与请求频繁。
 *
 * <p>计数必须使用 {@link StringRedisTemplate}：Redis 的 INCRBY 直接写入原始整数、
 * 不经过值序列化器，若用 JSON 序列化的 RedisTemplate 读取，会因缺少类型信息而反序列化失败。</p>
 *
 * <p>读取计数只做普通 GET，不使用 {@code increment(key, 0)}，避免读取动作在键不存在时
 * 创建没有过期时间的键。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 校验用户名与来源 IP 的失败次数是否已达上限。
     *
     * @param username    登录名
     * @param clientIp    客户端 IP，为空时跳过 IP 维度
     * @param userMaxFail 用户名维度阈值
     * @param ipMaxFail   IP 维度阈值
     */
    public void validate(String username, String clientIp, int userMaxFail, int ipMaxFail) {
        ensureBelowLimit(CacheConstants.LOGIN_FAIL_KEY_PREFIX + username, userMaxFail, ErrorCode.USER_LOCKED);
        if (StrUtil.isNotBlank(clientIp)) {
            ensureBelowLimit(CacheConstants.LOGIN_FAIL_IP_KEY_PREFIX + clientIp, ipMaxFail,
                    ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /** 记录一次失败（用户名与 IP 两个维度同时计数） */
    public void onLoginFail(String username, String clientIp) {
        increase(CacheConstants.LOGIN_FAIL_KEY_PREFIX + username);
        if (StrUtil.isNotBlank(clientIp)) {
            increase(CacheConstants.LOGIN_FAIL_IP_KEY_PREFIX + clientIp);
        }
    }

    /**
     * 登录成功：清零用户名维度计数。
     *
     * <p>IP 维度不清零，避免同一来源通过反复登录成功抹掉对其他账号的失败记录。</p>
     */
    public void onLoginSuccess(String username) {
        stringRedisTemplate.delete(CacheConstants.LOGIN_FAIL_KEY_PREFIX + username);
    }

    private void ensureBelowLimit(String key, int maxFail, ErrorCode errorCode) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(value)) {
            return;
        }
        if (NumberUtil.parseInt(value, 0) >= maxFail) {
            throw new BusinessException(errorCode);
        }
    }

    private void increase(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key, 1);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(SecurityConstants.LOGIN_LOCK_DURATION_SECONDS));
        }
    }
}
