package com.integrator.common.exception;

import com.integrator.common.error.ErrorCode;

public class UpstreamException extends IntegratorException {
    public UpstreamException(String message) {
        super(ErrorCode.UPSTREAM_ERROR, message);
    }
}
