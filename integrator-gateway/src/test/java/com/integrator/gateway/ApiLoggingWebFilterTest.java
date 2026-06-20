package com.integrator.gateway;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.integrator.common.observability.CorrelationConstants;
import com.integrator.gateway.config.ApiLoggingWebFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLoggingWebFilterTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final ApiLoggingWebFilter filter = new ApiLoggingWebFilter(objectMapper);

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ApiLoggingWebFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Logs gateway and downstream entries while keeping request and response bodies readable")
    void filterSuccessLogsBodiesAndMasksAuthorization() {
        String requestBody = "{\"id\":2}";
        String responseBody = "{\"ok\":true}";
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/agw/orders/123")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
        );
        exchange.getAttributes().put(CorrelationConstants.MDC_CORRELATION_ID_KEY, "corr-1");
        filter.filter(exchange, chainExchange -> DataBufferUtils.join(chainExchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    forwardedBody.set(new String(bytes, StandardCharsets.UTF_8));

                    chainExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute());
                    chainExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, URI.create("http://localhost:9000/order/2"));
                    chainExchange.getResponse().setStatusCode(HttpStatus.OK);
                    chainExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return chainExchange.getResponse().writeWith(Mono.just(
                            chainExchange.getResponse().bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8))
                    ));
                })).block();
        assertThat(forwardedBody.get()).isEqualTo(requestBody);
        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(responseBody);
        List<String> logs = logMessages();
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0))
                .contains("\"service\":\"integrator-gateway\"")
                .contains("\"correlationId\":\"corr-1\"")
                .contains("\"requestBody\":\"{\\\"id\\\":2}\"")
                .contains("\"responseBody\":\"{\\\"ok\\\":true}\"")
                .contains("\"statusCode\":200")
                .contains("\"result\":\"SUCCESS\"")
                .contains("\"Authorization\":[\"Bearer ***\"]")
                .doesNotContain("secret-token");
        assertThat(logs.get(1))
                .contains("\"event\":\"gateway.downstream\"")
                .contains("\"targetUrl\":\"http://localhost:9000/order/2\"")
                .contains("\"requestBody\":\"{\\\"id\\\":2}\"")
                .contains("\"responseBody\":\"{\\\"ok\\\":true}\"")
                .doesNotContain("secret-token");
    }

    @Test
    @DisplayName("Decompresses gzip response body for logging without changing the client response")
    void filterSuccessLogsReadableGzipResponseBody() throws Exception {
        String responseBody = "{\"compressed\":true}";
        byte[] gzippedBody = gzip(responseBody);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/agw/orders/123").build()
        );
        filter.filter(exchange, chainExchange -> {
            chainExchange.getResponse().setStatusCode(HttpStatus.OK);
            chainExchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_ENCODING, "gzip");
            return chainExchange.getResponse().writeWith(Mono.just(
                    chainExchange.getResponse().bufferFactory().wrap(gzippedBody)
            ));
        }).block();
        assertThat(exchange.getResponse().getBodyAsString().block()).startsWith("\u001F");
        assertThat(logMessages())
                .hasSize(1)
                .first()
                .asString()
                .contains("\"responseBody\":\"{\\\"compressed\\\":true}\"");
    }

    @Test
    @DisplayName("Uses gateway error body for gateway log but keeps downstream response empty")
    void filterSuccessKeepsDownstreamResponseEmptyWhenGatewayErrorBodyIsFallback() {
        String requestBody = "{\"id\":4}";
        String gatewayErrorBody = "{\"error\":\"Bad Gateway\"}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/agw/orders/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
        );
        filter.filter(exchange, chainExchange -> {
            chainExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute());
            chainExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, URI.create("http://localhost:9000/order/error"));
            chainExchange.getAttributes().put("gateway.errorResponseBody", gatewayErrorBody);
            chainExchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
            return Mono.empty();
        }).block();
        List<String> logs = logMessages();
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0))
                .contains("\"statusCode\":502")
                .contains("\"responseBody\":\"{\\\"error\\\":\\\"Bad Gateway\\\"}\"")
                .contains("\"result\":\"SERVER_ERROR\"");
        assertThat(logs.get(1))
                .contains("\"event\":\"gateway.downstream\"")
                .contains("\"targetUrl\":\"http://localhost:9000/order/error\"")
                .contains("\"responseBody\":\"\"");
    }

    private Route gatewayRoute() {
        return Route.async()
                .id("route-1")
                .uri("http://localhost:9000")
                .predicate(exchange -> true)
                .build();
    }

    private List<String> logMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private byte[] gzip(String value) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }
}
