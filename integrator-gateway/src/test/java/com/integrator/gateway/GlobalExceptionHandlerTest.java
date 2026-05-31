package com.integrator.gateway;

import com.integrator.common.observability.CorrelationConstants;
import com.integrator.gateway.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(objectMapper);

    @Test
    @DisplayName("Internal server error response is written for generic exception")
    void handleSuccessInternalServerError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/agw/orders").build()
        );
        exchange.getAttributes().put(CorrelationConstants.MDC_CORRELATION_ID_KEY, "corr-1");
        handler.handle(exchange, new RuntimeException("exception")).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("corr-1")
                .contains("/agw/orders")
                .contains("Internal Server Error");
    }

    @Test
    @DisplayName("Not found response is written for missing resource")
    void handleSuccessNotFound() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/missing").build()
        );
        handler.handle(exchange, new NoResourceFoundException(URI.create("/missing"), "/missing")).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("/missing")
                .contains("Not Found");
    }

    @Test
    @DisplayName("Original exception is returned when response is already committed")
    void handleFailureResponseAlreadyCommitted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/agw/orders").build()
        );
        RuntimeException exception = new RuntimeException("boom");
        exchange.getResponse().setComplete().block();
        assertThatThrownBy(() -> handler.handle(exchange, exception).block())
                .isSameAs(exception);
    }
}
