package com.qkit.framework.repeat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解。标注在写接口（create/update/delete）方法上，AOP 自动拦截。
 *
 * <p>基于 Redis {@code SET NX EX} 实现：窗口期（interval）内相同维度请求会被拒绝，
 * 返回 {@code 429 请勿重复提交}。锁保留到自然过期，不随方法执行结束提前释放。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /** 防重窗口（秒），默认 3 秒 */
    long interval() default 3;

    /** 防重粒度：默认按登录用户（未登录自动降级为 IP） */
    RepeatType type() default RepeatType.USER;

    /** 是否将请求参数摘要纳入 key。默认 false：同一用户/接口连点即拦；true：仅相同参数重复提交才拦 */
    boolean useParams() default false;
}