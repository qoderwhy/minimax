package com.qkit.common.log.spi;

import java.time.LocalDateTime;

/**
 * 操作日志记录模型。字段与 {@code sys_oper_log} 表一一对应，
 * 由切面在同步线程组装，交由 {@link OperLogSink} 各实现决定如何落库。
 *
 * @param module        模块名
 * @param name          操作名
 * @param userId        操作人ID（未登录为 null）
 * @param username      操作人账号（切面不查库，可保持 null 由实现回填）
 * @param ip            客户端 IP
 * @param userAgent     客户端 UA
 * @param method        类.方法
 * @param requestUrl    请求 URL
 * @param requestMethod HTTP 方法
 * @param requestParam  入参 JSON（已脱敏；saveParams=false 时为 null）
 * @param responseResult 返回结果摘要（saveResult=false 时为 null）
 * @param status        0=失败 1=成功
 * @param errorMsg      异常信息（截断；成功为 null）
 * @param costMs        耗时（毫秒）
 * @param operTime      操作时间
 */
public record OperLogRecord(
        String module,
        String name,
        Long userId,
        String username,
        String ip,
        String userAgent,
        String method,
        String requestUrl,
        String requestMethod,
        String requestParam,
        String responseResult,
        Integer status,
        String errorMsg,
        Long costMs,
        LocalDateTime operTime) {
}