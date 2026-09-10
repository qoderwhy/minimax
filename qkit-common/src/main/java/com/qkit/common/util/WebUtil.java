package com.qkit.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Web 工具类（兼容 Spring Boot 3 / Jakarta Servlet）。
 */
public final class WebUtil {

    /**
     * 可信反向代理地址模式，支持三种写法：
     * <ul>
     *     <li>精确地址：{@code 127.0.0.1}、{@code 0:0:0:0:0:0:0:1}</li>
     *     <li>IPv4 前缀：{@code 172.17.}</li>
     *     <li>IPv4 CIDR：{@code 172.16.0.0/12}</li>
     * </ul>
     * 为空表示不信任任何代理。由框架层在启动时注入。
     */
    private static volatile List<String> trustedProxyPatterns = List.of();

    private WebUtil() {
    }

    /** 注入可信代理配置（由框架层在启动时调用） */
    public static void setTrustedProxyPatterns(Collection<String> patterns) {
        List<String> cleaned = new ArrayList<>();
        if (patterns != null) {
            for (String pattern : patterns) {
                if (StrUtil.isNotBlank(pattern)) {
                    cleaned.add(pattern.trim());
                }
            }
        }
        trustedProxyPatterns = List.copyOf(cleaned);
    }

    /**
     * 解析客户端真实 IP。
     *
     * <p>只有直连来源（{@code remoteAddr}）命中可信代理配置时才采信
     * {@code X-Forwarded-For} / {@code X-Real-IP}，否则直接返回直连地址，
     * 防止请求方自行构造请求头伪造来源 IP。</p>
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded) && !"unknown".equalsIgnoreCase(forwarded)) {
            int comma = forwarded.indexOf(',');
            String candidate = comma > -1 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            if (StrUtil.isNotBlank(candidate)) {
                return candidate;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp) && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    /** 判断来源地址是否属于可信代理 */
    public static boolean isTrustedProxy(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        for (String pattern : trustedProxyPatterns) {
            if (matches(pattern, ip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String pattern, String ip) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.indexOf('/') > -1) {
            return matchesCidr(pattern, ip);
        }
        if (pattern.endsWith(".")) {
            return ip.startsWith(pattern);
        }
        return pattern.equalsIgnoreCase(ip);
    }

    /** 仅支持 IPv4 CIDR；IPv6 请使用精确地址配置 */
    private static boolean matchesCidr(String pattern, String ip) {
        int slash = pattern.indexOf('/');
        long base = ipv4ToLong(pattern.substring(0, slash));
        long target = ipv4ToLong(ip);
        if (base < 0 || target < 0) {
            return false;
        }
        int bits;
        try {
            bits = Integer.parseInt(pattern.substring(slash + 1).trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (bits < 0 || bits > 32) {
            return false;
        }
        long mask = bits == 0 ? 0L : (0xFFFFFFFFL << (32 - bits)) & 0xFFFFFFFFL;
        return (target & mask) == (base & mask);
    }

    /** 点分十进制 IPv4 转 long；非法或不支持的形式返回 -1 */
    private static long ipv4ToLong(String ip) {
        if (StrUtil.isBlank(ip)) {
            return -1L;
        }
        String[] segments = ip.trim().split("\\.");
        if (segments.length != 4) {
            return -1L;
        }
        long value = 0L;
        for (String segment : segments) {
            int part;
            try {
                part = Integer.parseInt(segment);
            } catch (NumberFormatException e) {
                return -1L;
            }
            if (part < 0 || part > 255) {
                return -1L;
            }
            value = (value << 8) | part;
        }
        return value;
    }
}
