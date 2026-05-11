package com.integrator.common.exception;

import com.integrator.common.error.ErrorCode;

public class ResourceNotFoundException extends IntegratorException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
