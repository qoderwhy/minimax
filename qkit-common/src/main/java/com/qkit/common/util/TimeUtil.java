package com.qkit.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.exception.BusinessException;

import java.time.LocalDateTime;

/**
 * 时间参数解析工具：把查询条件中的字符串时间解析为 {@link LocalDateTime}。
 *
 * <p>兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种格式，
 * 避免依赖数据库对字符串与时间列做隐式转换。</p>
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * 解析可空时间参数。
     *
     * @param text 前端传入的时间字符串，空白视为未传
     * @return 解析结果；入参为空时返回 {@code null}
     * @throws BusinessException 格式无法识别时抛出 {@link ErrorCode#TIME_FORMAT_INVALID}，由全局异常处理统一封装
     */
    public static LocalDateTime parseNullable(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return DateUtil.parse(text.trim()).toLocalDateTime();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TIME_FORMAT_INVALID, text);
        }
    }
}
