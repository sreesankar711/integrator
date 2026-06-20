package com.integrator.gateway.exception;

import com.integrator.common.api.GatewayErrorResponse;
import com.integrator.common.observability.CorrelationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        return Mono.deferContextual(contextView -> writeErrorResponse(exchange, ex, contextView));
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, Throwable ex, ContextView contextView) {
        String correlationId = correlationId(exchange, contextView);
        log.error("Unhandled gateway exception: correlationId={}, method={}, uri={}",
                correlationId, exchange.getRequest().getMethod(), exchange.getRequest().getURI(), ex);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof NoResourceFoundException) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else if (ex instanceof java.net.ConnectException) {
            httpStatus = HttpStatus.BAD_GATEWAY;
        }
        GatewayErrorResponse gatewayErrorResponse = GatewayErrorResponse.builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .path(exchange.getRequest().getPath().value())
                .error(httpStatus.isSameCodeAs(HttpStatus.BAD_GATEWAY) || httpStatus.is4xxClientError()
                        ? httpStatus.getReasonPhrase() + ": " + exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)
                        : httpStatus.getReasonPhrase())
                .build();

        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        byte[] bytes = objectMapper.writeValueAsBytes(gatewayErrorResponse);
        exchange.getAttributes().put("gateway.errorResponseBody", new String(bytes, StandardCharsets.UTF_8));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String correlationId(ServerWebExchange exchange, ContextView contextView) {
        String correlationId = exchange.getAttribute(CorrelationConstants.MDC_CORRELATION_ID_KEY);
        if (correlationId != null &&  !correlationId.isEmpty()) {
            return correlationId;
        }
        if (contextView.hasKey(CorrelationConstants.MDC_CORRELATION_ID_KEY)) {
            return contextView.get(CorrelationConstants.MDC_CORRELATION_ID_KEY);
        }
        correlationId = MDC.get(CorrelationConstants.MDC_CORRELATION_ID_KEY);
        return Objects.requireNonNullElse(correlationId, "");
    }
}
