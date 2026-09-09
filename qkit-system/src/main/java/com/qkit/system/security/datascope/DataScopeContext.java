package com.qkit.system.security.datascope;

import com.qkit.system.enums.DataScopeEnum;
import lombok.Data;

import java.util.List;

/**
 * 数据权限上下文，由 AOP 切面在 @DataScope 方法进入时填充，handler 读取后拼装 SQL 条件。
 */
@Data
public class DataScopeContext {

    private Long userId;

    private DataScopeEnum scope;

    /** 可见部门 ID 集合；scope=ALL 时为 null，scope=SELF 时为 [当前部门ID] */
    private List<Long> visibleDeptIds;

    /** 可见用户 ID 集合；scope=ALL 时为 null，scope=SELF 时为 [userId] */
    private List<Long> visibleUserIds;

    /** 注解声明的参与过滤的表名 */
    private String table;

    /** 注解声明的部门维度列 */
    private String deptColumn;

    /** 注解声明的用户维度列 */
    private String userColumn;
}
