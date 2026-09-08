package com.qkit.common.exception;

import com.qkit.common.api.ErrorCode;
import lombok.Getter;

/**
 * 系统异常：需记录 ERROR 日志并告警，禁止用于控制正常业务流程。
 */
@Getter
public sealed class SystemException extends RuntimeException permits SystemException.Unchecked {

    private static final long serialVersionUID = 1L;

    private final int code;

    public SystemException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    /**
     * 未检查系统异常：默认错误码 500，用于不可预期的系统级错误。
     */
    public static final class Unchecked extends SystemException {

        private static final long serialVersionUID = 1L;

        public Unchecked(String message, Throwable cause) {
            super(500, message, cause);
        }
    }
}