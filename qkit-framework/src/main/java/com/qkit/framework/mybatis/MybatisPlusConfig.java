package com.qkit.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.qkit.common.constant.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：数据权限、分页插件、乐观锁、防止全表更新、公共字段自动填充。
 */
@Slf4j
@Component
public class MybatisPlusConfig {

    /**
     * 拦截器链：数据权限 → 分页 → 乐观锁 → 防全表更新。
     * DataPermissionInterceptor 必须在分页之前（先追加 WHERE 再分页优化）。
     */
    @org.springframework.context.annotation.Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<DataPermissionHandler> dataPermissionHandlerProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限拦截器（可选，无 handler 实现时跳过）
        dataPermissionHandlerProvider.ifAvailable(handler -> {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(handler));
            log.info("已注册数据权限拦截器 DataPermissionInterceptor");
        });
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 公共字段自动填充。创建/更新时自动注入创建人、创建时间等。
     */
    @org.springframework.context.annotation.Bean
    public MetaObjectHandler autoFillHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Long userId = currentUserId();
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createBy", Long.class, userId);
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateBy", Long.class, userId);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long userId = currentUserId();
                LocalDateTime now = LocalDateTime.now();
                strictUpdateFill(metaObject, "updateBy", Long.class, userId);
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            private Long currentUserId() {
                try {
                    return cn.dev33.satoken.stp.StpUtil.isLogin()
                            ? cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong()
                            : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            }
        };
    }
}
