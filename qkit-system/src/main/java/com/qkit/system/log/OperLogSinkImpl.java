package com.qkit.system.log;

import com.qkit.common.log.spi.OperLogRecord;
import com.qkit.common.log.spi.OperLogSink;
import com.qkit.system.domain.entity.OperLog;
import com.qkit.system.domain.entity.User;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 操作日志落库实现：将 SPI 记录映射为 {@code sys_oper_log} 实体，
 * 交由 {@link OperLogService#asyncSave} 异步写入。
 */
@Component
@RequiredArgsConstructor
public class OperLogSinkImpl implements OperLogSink {

    private final OperLogService operLogService;
    private final UserMapper userMapper;

    @Override
    public void record(OperLogRecord r) {
        OperLog entity = new OperLog();
        entity.setModule(r.module());
        entity.setName(r.name());
        entity.setUserId(r.userId());
        entity.setUsername(resolveUsername(r));
        entity.setIp(r.ip());
        entity.setUserAgent(r.userAgent());
        entity.setMethod(r.method());
        entity.setRequestUrl(r.requestUrl());
        entity.setRequestMethod(r.requestMethod());
        entity.setRequestParam(r.requestParam());
        entity.setResponseResult(r.responseResult());
        entity.setStatus(r.status());
        entity.setErrorMsg(r.errorMsg());
        entity.setCostMs(r.costMs());
        entity.setOperTime(r.operTime());
        operLogService.asyncSave(entity);
    }

    /** 切面只携带 userId，此处按 userId 回查用户名（未登录为 null，写出时走库默认值） */
    private String resolveUsername(OperLogRecord r) {
        if (r.username() != null) return r.username();
        if (r.userId() == null) return null;
        User user = userMapper.selectById(r.userId());
        return user == null ? null : user.getUsername();
    }
}