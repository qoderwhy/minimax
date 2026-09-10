package com.qkit.framework.web;

import com.qkit.common.util.WebUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web 配置：CORS 与可信反向代理。
 *
 * <p>允许来源通过 {@code app.cors.allowed-origin-patterns} 配置，默认仅放行本机（开发场景）。
 * 生产环境请覆盖为具体域名，切勿使用 {@code *}——与 {@code allowCredentials(true)} 组合会引入安全风险。</p>
 *
 * <p>可信代理通过 {@code app.security.trusted-proxies} 配置，支持精确 IP、IPv4 前缀与 IPv4 CIDR；
 * 未命中可信代理时忽略 {@code X-Forwarded-For}，防止伪造来源 IP。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    /** 可信反向代理地址，逗号分隔；默认仅信任本机回环地址 */
    @Value("${app.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}")
    private String[] trustedProxies;

    @PostConstruct
    public void initTrustedProxies() {
        WebUtil.setTrustedProxyPatterns(Arrays.asList(trustedProxies));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("satoken")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
