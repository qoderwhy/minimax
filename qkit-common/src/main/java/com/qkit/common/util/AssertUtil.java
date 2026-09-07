package com.qkit.common.util;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.Objects;

/**
 * Assert 工具类。校验失败时抛出 {@link com.qkit.common.exception.BusinessException}。
 */
public final class AssertUtil {

    private AssertUtil() {
    }

    public static void notNull(Object obj, com.qkit.common.api.ErrorCode errorCode) {
        if (obj == null) {
            throw new com.qkit.common.exception.BusinessException(errorCode);
        }
    }

    public static void hasText(String str, com.qkit.common.api.ErrorCode errorCode) {
        if (str == null || str.isBlank()) {
            throw new com.qkit.common.exception.BusinessException(errorCode);
        }
    }

    public static void notEmpty(List<?> list, com.qkit.common.api.ErrorCode errorCode) {
        if (CollUtil.isEmpty(list)) {
            throw new com.qkit.common.exception.BusinessException(errorCode);
        }
    }

    public static void isTrue(boolean condition, com.qkit.common.api.ErrorCode errorCode) {
        if (!condition) {
            throw new com.qkit.common.exception.BusinessException(errorCode);
        }
    }

    public static void equals(Object a, Object b, com.qkit.common.api.ErrorCode errorCode) {
        if (!Objects.equals(a, b)) {
            throw new com.qkit.common.exception.BusinessException(errorCode);
        }
    }
}
