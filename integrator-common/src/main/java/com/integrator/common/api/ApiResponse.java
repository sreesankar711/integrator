package com.integrator.common.api;

import com.integrator.common.observability.CorrelationConstants;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.MDC;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    @Builder.Default
    private String correlationId = MDC.get(CorrelationConstants.MDC_CORRELATION_ID_KEY);
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }


    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .build();
    }

}
