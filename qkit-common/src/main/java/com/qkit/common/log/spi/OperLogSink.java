package com.qkit.common.log.spi;

/**
 * 操作日志落库 SPI。框架层的 {@code OperLogAspect} 组装 {@link OperLogRecord}
 * 后调用本接口，由业务侧（如 qkit-system）提供实现并异步写入数据库。
 *
 * <p>通过 {@code ObjectProvider<OperLogSink>} 获取，未提供实现时切面退化为控制台日志。</p>
 */
public interface OperLogSink {

    /** 记录一条操作日志。实现侧可自行决定是否异步（如调用 @Async 的 Service 方法） */
    void record(OperLogRecord record);
}