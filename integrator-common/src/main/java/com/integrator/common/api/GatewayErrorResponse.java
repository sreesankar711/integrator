package com.integrator.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayErrorResponse {
    private Instant timestamp;
    private String correlationId;
    private String path;
    private String error;
}
