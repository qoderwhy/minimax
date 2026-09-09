package com.qkit.system.security.datascope;

import cn.dev33.satoken.stp.StpUtil;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.enums.DataScopeEnum;
import com.qkit.system.service.PermissionService;
import com.qkit.system.service.impl.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面：进入 @DataScope 方法时，计算当前用户的数据范围上下文并放入 ThreadLocal，
 * MyBatis-Plus DataPermissionInterceptor 读取上下文自动追加 SQL 条件。
 *
 * <p>关键：计算阶段 Holder 为空，helper 内部查询（sys_dept / sys_user）不会被过滤。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataScopeHelper dataScopeHelper;
    private final PermissionService permissionService;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        try {
            // 仅登录态才启用
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                DataScopeContext ctx = buildContext(userId, dataScope);
                DataScopeContextHolder.set(ctx);
            }
            return pjp.proceed();
        } finally {
            DataScopeContextHolder.clear();
        }
    }

    /**
     * 构建上下文：此时 Holder 为 null，helper 内部查询不会触发过滤。
     */
    private DataScopeContext buildContext(Long userId, DataScope dataScope) {
        DataScopeContext ctx = new DataScopeContext();
        ctx.setUserId(userId);
        ctx.setTable(dataScope.table());
        ctx.setDeptColumn(dataScope.deptColumn());
        ctx.setUserColumn(dataScope.userColumn());
        // 计算数据范围（直接查库，不经过 handler 过滤）
        DataScopeEnum scope = permissionService.getDataScope(userId);
        ctx.setScope(scope);
        ctx.setVisibleDeptIds(dataScopeHelper.visibleDeptIds(userId));
        ctx.setVisibleUserIds(dataScopeHelper.visibleUserIds(userId));
        return ctx;
    }
}
