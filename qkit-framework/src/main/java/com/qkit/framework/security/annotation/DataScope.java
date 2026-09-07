package com.qkit.framework.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（5 级）。标注在 Service 方法上，AOP 拦截并改写 SQL。
 *
 * <p>MVP 默认仅 {@code sys_user} 演示完整链路（详见 06 §4.1）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 表别名，SQL 中 AS 的别名 */
    String alias() default "";

    /** 部门字段名，默认 dept_id */
    String deptColumn() default "dept_id";

    /** 用户字段名，默认 create_by */
    String userColumn() default "create_by";
}
