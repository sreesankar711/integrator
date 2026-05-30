package com.integrator.common.observability;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = UUID.randomUUID().toString();
        String xCorrelationId = exchange.getRequest().getHeaders().getFirst(CorrelationConstants.CORRELATION_ID_HEADER);
        if (xCorrelationId != null) 
            exchange.getResponse().getHeaders().set(CorrelationConstants.CORRELATION_ID_HEADER, xCorrelationId);

        exchange.getAttributes().put(
                CorrelationConstants.MDC_CORRELATION_ID_KEY,
                correlationId
        );
        return chain.filter(exchange)
                .contextWrite(context -> context
                    .put(CorrelationConstants.MDC_CORRELATION_ID_KEY, correlationId)
                    .put(CorrelationConstants.CORRELATION_ID_HEADER, xCorrelationId != null ? xCorrelationId : "")
                );
    }
}
