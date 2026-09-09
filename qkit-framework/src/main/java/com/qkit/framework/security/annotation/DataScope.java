package com.qkit.framework.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（5 级）。标注在 Service 查询方法上，AOP 切面计算上下文，
 * MyBatis-Plus DataPermissionInterceptor 在 SQL 执行前自动追加过滤条件。
 *
 * <p>示例：</p>
 * <ul>
 *     <li>用户表：{@code @DataScope(table="sys_user", deptColumn="dept_id", userColumn="create_by")}</li>
 *     <li>日志表：{@code @DataScope(table="sys_login_log", userColumn="user_id")}</li>
 *     <li>部门表：{@code @DataScope(table="sys_dept", deptColumn="id")}</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 参与过滤的表名（必须与 SQL 中表名一致，如 sys_user） */
    String table() default "";

    /** 部门维度列名（如 dept_id / id），支持 DEPT/DEPT_AND_CHILD/CUSTOM 级别 */
    String deptColumn() default "";

    /** 用户维度列名（如 create_by / user_id），支持 SELF 级别；DEPT 级别回退用 */
    String userColumn() default "";
}
