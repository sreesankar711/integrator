package com.integrator.gateway.config;

import com.integrator.common.observability.CorrelationIdWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    @Bean
    public CorrelationIdWebFilter correlationIdWebFilter() {
        return new CorrelationIdWebFilter();
    }
}
