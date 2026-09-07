package com.qkit.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.qkit.common.constant.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：分页插件、乐观锁、防止全表更新、公共字段自动填充。
 */
@Slf4j
@Component
public class MybatisPlusConfig {

    /**
     * 分页插件、乐观锁、防全表更新插件。
     */
    @org.springframework.context.annotation.Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
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
