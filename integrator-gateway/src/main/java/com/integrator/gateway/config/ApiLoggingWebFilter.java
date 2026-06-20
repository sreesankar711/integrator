package com.integrator.gateway.config;

import com.integrator.common.observability.CorrelationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

@Slf4j
public class ApiLoggingWebFilter implements WebFilter {

    private static final String MASKED_AUTHORIZATION = "Bearer ***";
    private final ObjectMapper objectMapper;

    public ApiLoggingWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant start = Instant.now();
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        AtomicReference<String> requestBody = new AtomicReference<>("");
        AtomicReference<String> responseBody = new AtomicReference<>("");
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .switchIfEmpty(Mono.defer(() -> Mono.just(bufferFactory.wrap(new byte[0]))))
                .flatMap(dataBuffer -> {
                    byte[] requestBytes = readBytes(dataBuffer);
                    requestBody.set(body(requestBytes, exchange.getRequest().getHeaders()));
                    ServerHttpRequest decoratedRequest = decorateRequest(exchange, requestBytes, bufferFactory);
                    ServerHttpResponse decoratedResponse = decorateResponse(exchange, responseBody, bufferFactory);
                    ServerWebExchange decoratedExchange = exchange.mutate()
                            .request(decoratedRequest)
                            .response(decoratedResponse)
                            .build();
                    return chain.filter(decoratedExchange)
                            .doFinally(signalType -> {
                                        String finalResponseBody = responseBody.get();
                                        int statusCode = statusCode(decoratedExchange);
                                        if ((finalResponseBody == null || finalResponseBody.isBlank()) && statusCode >= 400) {
                                            finalResponseBody = decoratedExchange.getAttributeOrDefault("gateway.errorResponseBody", "");
                                        }
                                        logGatewayEvent(decoratedExchange, start, requestBody.get(), finalResponseBody);
                                        if (isDownstreamProxyCall(decoratedExchange)) {
                                            logDownstreamProxyCall(decoratedExchange, start, requestBody.get(), responseBody.get());
                                        }
                                    }
                            );
                });
    }

    private ServerHttpRequest decorateRequest(ServerWebExchange exchange, byte[] requestBytes, DataBufferFactory bufferFactory) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                if (requestBytes.length == 0) {
                    return Flux.empty();
                }
                return Flux.defer(() -> Flux.just(bufferFactory.wrap(requestBytes)));
            }
        };
    }

    private ServerHttpResponse decorateResponse(ServerWebExchange exchange, AtomicReference<String> responseBody, DataBufferFactory bufferFactory) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            byte[] responseBytes = readBytes(dataBuffer);
                            responseBody.set(body(responseBytes, getHeaders()));
                            DataBuffer wrappedBuffer = bufferFactory.wrap(responseBytes);
                            return super.writeWith(Mono.just(wrappedBuffer));
                        });
            }
        };
    }

    private byte[] readBytes(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return bytes;
    }

    private void logGatewayEvent(ServerWebExchange exchange, Instant start, String requestBody, String responseBody) {
        try {
            log.info(objectMapper.writeValueAsString(logEvent(exchange, start, requestBody, responseBody)));
        } catch (Exception ex) {
            log.warn("Failed to write gateway API log: {}", ex.getLocalizedMessage());
        }
    }

    private void logDownstreamProxyCall(ServerWebExchange exchange, Instant start, String requestBody, String responseBody) {
        try {
            log.info(objectMapper.writeValueAsString(downstreamProxyLogEvent(exchange, start, requestBody, responseBody)));
        } catch (Exception ex) {
            log.warn("Failed to write downstream proxy log: {}", ex.getLocalizedMessage());
        }
    }

    private Map<String, Object> logEvent(ServerWebExchange exchange, Instant start, String requestBody, String responseBody) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("service", "integrator-gateway");
        event.put("correlationId", exchange.getAttribute(CorrelationConstants.MDC_CORRELATION_ID_KEY));
        event.put("method", request.getMethod().name());
        event.put("path", request.getPath().value());
        event.put("queryString", request.getURI().getRawQuery());
        event.put("requestHeaders", headers(request.getHeaders()));
        event.put("responseHeaders", headers(exchange.getResponse().getHeaders()));
        event.put("requestBody", requestBody);
        event.put("responseBody", responseBody);
        event.put("routeId", routeId(exchange));
        event.put("targetUrl", targetUrl(exchange));
        event.put("statusCode", statusCode(exchange));
        event.put("durationMs", Duration.between(start, Instant.now()).toMillis());
        event.put("result", result(statusCode(exchange)));
        return event;
    }

    private Map<String, Object> downstreamProxyLogEvent(ServerWebExchange exchange, Instant start, String requestBody, String responseBody) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "gateway.downstream");
        event.put("service", "integrator-gateway");
        event.put("correlationId", exchange.getAttribute(CorrelationConstants.MDC_CORRELATION_ID_KEY));
        event.put("method", request.getMethod().name());
        event.put("path", request.getPath().value());
        event.put("routeId", routeId(exchange));
        event.put("targetUrl", targetUrl(exchange));
        event.put("requestHeaders", headers(request.getHeaders()));
        event.put("responseHeaders", headers(exchange.getResponse().getHeaders()));
        event.put("requestBody", requestBody);
        event.put("responseBody", responseBody);
        event.put("statusCode", statusCode(exchange));
        event.put("durationMs", Duration.between(start, Instant.now()).toMillis());
        event.put("result", result(statusCode(exchange)));
        return event;
    }

    private Map<String, List<String>> headers(HttpHeaders headers) {
        Map<String, List<String>> maskedHeaders = new LinkedHashMap<>();
        headers.forEach((headerName, values) ->
                maskedHeaders.put(headerName, values.stream()
                        .map(value -> maskHeader(headerName, value))
                        .toList())
        );
        return maskedHeaders;
    }

    private String maskHeader(String headerName, String value) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
            return MASKED_AUTHORIZATION;
        }
        return value;
    }

    private String body(byte[] bytes, HttpHeaders headers) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        byte[] readableBytes = bytes;
        if (isGzip(headers)) {
            readableBytes = gunzip(bytes);
        }
        Charset charset = headers.getContentType() == null || headers.getContentType().getCharset() == null
                ? StandardCharsets.UTF_8
                : headers.getContentType().getCharset();
        return new String(readableBytes, charset);
    }

    private boolean isGzip(HttpHeaders headers) {
        List<String> contentEncoding = headers.get(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding == null || contentEncoding.isEmpty()) {
            return false;
        }
        return contentEncoding.stream()
                .anyMatch(value -> value != null && value.toLowerCase().contains("gzip"));
    }

    private byte[] gunzip(byte[] bytes) {
        try (
             ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)
        ) {
            return gzipInputStream.readAllBytes();
        } catch (IOException ex) {
            return bytes;
        }
    }

    private String routeId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route == null ? null : route.getId();
    }

    private String targetUrl(ServerWebExchange exchange) {
        URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        return targetUri == null ? null : targetUri.toString();
    }

    private int statusCode(ServerWebExchange exchange) {
        HttpStatusCode httpStatusCode = exchange.getResponse().getStatusCode();
        if (httpStatusCode != null) {
            return httpStatusCode.value();
        }
        return 200;
    }

    private boolean isDownstreamProxyCall(ServerWebExchange exchange) {
        return exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR) != null
                && exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR) != null;
    }

    private String result(int statusCode) {
        return switch (statusCode / 100) {
            case 2 -> "SUCCESS";
            case 3 -> "REDIRECTION";
            case 4 -> "CLIENT_ERROR";
            case 5 -> "SERVER_ERROR";
            default -> "UNKNOWN";
        };
    }
}
