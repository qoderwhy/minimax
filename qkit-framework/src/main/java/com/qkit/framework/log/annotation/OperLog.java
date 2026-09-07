package com.qkit.framework.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。标注在 Controller 方法上，AOP 自动记录。
 *
 * <p>两个参数均必填，对应 {@code sys_oper_log.module / name}。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块名，例如 "用户管理" */
    String module();

    /** 操作名，例如 "新增用户" */
    String name();

    /** 是否保存请求参数（默认 true；登录密码等敏感接口可设为 false） */
    boolean saveParams() default true;

    /** 是否保存响应结果（默认 true；登录等敏感接口可设为 false） */
    boolean saveResult() default true;
}
