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
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/captcha",
                        "/auth/login",
                        "/auth/logout",
                        "/system/dict/type/**");
    }
}
