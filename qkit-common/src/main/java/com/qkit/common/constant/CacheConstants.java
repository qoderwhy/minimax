package com.qkit.common.constant;

/**
 * 缓存相关常量。
 */
public final class CacheConstants {

    /** 用户权限缓存：perm:{userId} → List<String> 权限码集合 */
    public static final String PERM_KEY_PREFIX = "perm:";

    /** 登录失败计数：login:fail:{username}，5 次失败锁定 10 分钟 */
    public static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    /** 图形验证码：captcha:{uuid}，3 分钟过期 */
    public static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /** 部门子树缓存：dept:child:{deptId}，TTL 5 分钟 */
    public static final String DEPT_CHILD_KEY_PREFIX = "dept:child:";

    /** 字典缓存：sys_dict:{type} → JSON 列表，TTL 永不过期 */
    public static final String DICT_KEY_PREFIX = "sys_dict:";

    private CacheConstants() {
    }
}
