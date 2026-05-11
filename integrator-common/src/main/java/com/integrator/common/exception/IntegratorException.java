package com.integrator.common.exception;

import com.integrator.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class IntegratorException extends RuntimeException {
    private final ErrorCode errorCode;

    public IntegratorException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public IntegratorException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}
