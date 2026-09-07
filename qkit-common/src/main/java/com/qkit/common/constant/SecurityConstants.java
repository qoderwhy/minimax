package com.qkit.common.constant;

/**
 * 安全相关常量。
 */
public final class SecurityConstants {

    /** Sa-Token 请求头名 */
    public static final String SA_TOKEN_HEADER = "satoken";

    /** 链路追踪 Header */
    public static final String TRACE_HEADER = "X-Trace-Id";

    /** 登录失败最大次数 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 登录锁定时长（秒）：10 分钟 */
    public static final long LOGIN_LOCK_DURATION_SECONDS = 10 * 60;

    private SecurityConstants() {
    }
}
