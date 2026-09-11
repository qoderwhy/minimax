package com.qkit.framework.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录校验拦截器（fail-closed：默认拒绝，仅显式排除的公开接口可匿名访问）。
 * 应用部署在 context-path {@code /admin-api} 下：Spring 拦截器匹配的是「去除 context-path 之后」的路径，
 * 故用 {@code /**} 表示「所有业务路径默认需登录」；更细粒度的按钮级权限由 {@code @SaCheckPermission} 控制。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器匹配的是「去除 context-path（/admin-api）之后」的路径，故此处全部用不含 /admin-api 的相对路径。
        // 切勿写成 /admin-api/**：那样会与 /admin-api/auth/** 一起匹配不到实际路径，导致接口被整体放行（fail-open）。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录相关：验证码 / 登录 / 登出（必须匿名可访问）
                        "/auth/captcha",
                        "/auth/login",
                        "/auth/logout",
                        // 登录页及公开下拉用到的字典项
                        "/system/dict/type/**",
                        // 接口文档：Knife4j / springdoc（prod 已整体关闭，dev 放行以便调试）
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/favicon.ico",
                        // 健康检查 / 指标：供探针与监控采集
                        "/actuator/**");
    }
}
