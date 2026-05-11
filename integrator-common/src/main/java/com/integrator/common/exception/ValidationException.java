package com.integrator.common.exception;

import com.integrator.common.error.ErrorCode;

public class ValidationException  extends IntegratorException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
