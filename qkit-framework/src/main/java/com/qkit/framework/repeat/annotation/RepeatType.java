package com.qkit.framework.repeat.annotation;

/**
 * 防重复提交粒度。
 */
public enum RepeatType {

    /** 按登录用户维度（未登录时自动降级为 IP） */
    USER,

    /** 按客户端 IP 维度 */
    IP,

    /** 全局维度（同一接口统一拦截） */
    ALL
}