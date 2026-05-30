package com.integrator.gateway.config;

import com.integrator.common.observability.CorrelationConstants;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropagationConfig {
    @PostConstruct
    public void registerCorrelationIdAccessor() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                CorrelationConstants.MDC_CORRELATION_ID_KEY,
                () -> MDC.get(CorrelationConstants.MDC_CORRELATION_ID_KEY),
                value -> MDC.put(CorrelationConstants.MDC_CORRELATION_ID_KEY, value),
                () -> MDC.remove(CorrelationConstants.MDC_CORRELATION_ID_KEY)
        );
    }
}
