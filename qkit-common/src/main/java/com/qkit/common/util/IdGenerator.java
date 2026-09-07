package com.qkit.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * ID 生成工具。基于雪花 ID，前端需配合 {@code @JsonSerialize(ToStringSerializer.class)} 防止精度丢失。
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static long nextId() {
        return IdUtil.getSnowflake(1, 1).nextId();
    }
}
