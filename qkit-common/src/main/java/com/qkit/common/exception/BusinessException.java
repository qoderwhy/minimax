package com.qkit.common.exception;

import com.qkit.common.api.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。Service 层校验失败时抛出，由全局异常拦截器统一封装为 {@code R.fail(...)}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode, String extraMessage) {
        super(errorCode.getMessage() + ": " + extraMessage);
        this.code = errorCode.getCode();
    }
}
