package com.integrator.gateway.config;

import com.integrator.common.observability.CorrelationIdWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ObservabilityConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorrelationIdWebFilter correlationIdWebFilter() {
        return new CorrelationIdWebFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public ApiLoggingWebFilter apiLoggingWebFilter(ObjectMapper objectMapper) {
        return new ApiLoggingWebFilter(objectMapper);
    }
}
