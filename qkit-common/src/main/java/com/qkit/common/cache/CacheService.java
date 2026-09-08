package com.qkit.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存操作封装。统一封装常见 Redis 存取删操作，业务模块直接注入使用，
 * 避免重复拼接 key 前缀与序列化细节。
 *
 * <p>key 由调用方拼好（如 {@code CacheConstants.DICT_KEY_PREFIX + type}），
 * 本类不做前缀拼接。</p>
 */
@Component
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 读取缓存，未命中返回 null。返回类型由反序列化结果决定 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /** 写入缓存（无过期时间） */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 写入缓存并指定过期时间 */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /** 写入缓存并指定过期秒数 */
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    /** 删除指定 key，不存在时静默忽略 */
    public void delete(String... keys) {
        if (keys == null || keys.length == 0) return;
        redisTemplate.delete(java.util.Arrays.asList(keys));
    }

    /** 判断 key 是否存在 */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}