package com.qkit.framework.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置：全局登录校验 + 注解鉴权。
 *
 * <p>采用 fail-closed（默认拒绝）模型：{@code /admin-api/**} 默认必须登录，
 * 只有显式排除的路径（验证码/登录/登出/公开字典接口）可匿名访问。
 * 业务接口再通过 {@code @SaCheckPermission} 做更细粒度的按钮级鉴权。</p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/admin-api/**")
                .excludePathPatterns(
                        "/admin-api/auth/captcha",
                        "/admin-api/auth/login",
                        "/admin-api/auth/logout",
                        "/admin-api/system/dict/type/**");
    }
}
