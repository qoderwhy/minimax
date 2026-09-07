package com.qkit.framework.ratelimit;

import com.qkit.common.constant.CacheConstants;
import com.qkit.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录限流：5 次失败锁定 10 分钟。
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 校验当前用户名失败次数是否已达上限。
     */
    public void validate(String username) {
        String key = CacheConstants.LOGIN_FAIL_KEY_PREFIX + username;
        Object count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count.toString()) >= SecurityConstants.MAX_LOGIN_FAIL_COUNT) {
            throw new com.qkit.common.exception.BusinessException(com.qkit.common.api.ErrorCode.USER_LOCKED);
        }
    }

    /**
     * 记录一次失败。
     */
    public void onLoginFail(String username) {
        String key = CacheConstants.LOGIN_FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(SecurityConstants.LOGIN_LOCK_DURATION_SECONDS));
        }
    }

    /**
     * 登录成功清零。
     */
    public void onLoginSuccess(String username) {
        redisTemplate.delete(CacheConstants.LOGIN_FAIL_KEY_PREFIX + username);
    }
}
