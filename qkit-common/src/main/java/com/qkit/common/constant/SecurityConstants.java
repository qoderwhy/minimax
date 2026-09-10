package com.qkit.common.constant;

/**
 * 安全相关常量。
 */
public final class SecurityConstants {

    /** Sa-Token 请求头名 */
    public static final String SA_TOKEN_HEADER = "satoken";

    /** 登录会话中存放登录账号的 key（AuthServiceImpl 登录成功时写入，操作日志等处读取） */
    public static final String SESSION_USERNAME = "username";

    /** 同一用户名登录失败最大次数（默认值，可被系统参数 sys.login.retryLimit 覆盖） */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 同一来源 IP 登录失败最大次数（默认值，可被系统参数 sys.login.ipRetryLimit 覆盖） */
    public static final int MAX_LOGIN_FAIL_IP_COUNT = 20;

    /** 登录锁定时长（秒）：10 分钟 */
    public static final long LOGIN_LOCK_DURATION_SECONDS = 10 * 60;

    private SecurityConstants() {
    }
}
