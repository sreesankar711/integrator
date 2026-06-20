package com.integrator.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
public class ApiLoggingFilter extends OncePerRequestFilter {
    private static final String MASKED_AUTHORIZATION = "Bearer ***";

    private final ObjectMapper objectMapper;
    private final String serviceName;

    public ApiLoggingFilter(ObjectMapper objectMapper, String serviceName) {
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, -1);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                log.info(objectMapper.writeValueAsString(logEvent(wrappedRequest, wrappedResponse, start)));
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private Map<String, Object> logEvent(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, Instant start) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("service", serviceName);
        event.put("correlationId", MDC.get(CorrelationConstants.MDC_CORRELATION_ID_KEY));
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());
        event.put("queryString", request.getQueryString());
        event.put("requestHeaders", requestHeaders(request));
        event.put("responseHeaders", responseHeaders(response));
        event.put("requestBody", body(request.getContentAsByteArray(), request.getCharacterEncoding()));
        event.put("responseBody", body(response.getContentAsByteArray(), response.getCharacterEncoding()));
        event.put("statusCode", response.getStatus());
        event.put("durationMs", Duration.between(start, Instant.now()).toMillis());
        event.put("result", result(response.getStatus()));
        return event;
    }

    private Map<String, List<String>> requestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                headers.put(headerName, Collections.list(request.getHeaders(headerName)).stream()
                        .map(value -> maskHeader(headerName, value))
                        .toList())
        );
        return headers;
    }

    private Map<String, List<String>> responseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String headerName : response.getHeaderNames()) {
            Collection<String> values = response.getHeaders(headerName);
            headers.put(headerName, values.stream()
                    .map(value -> maskHeader(headerName, value))
                    .toList());
        }
        return headers;
    }

    private String maskHeader(String headerName, String value) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
            return MASKED_AUTHORIZATION;
        }
        return value;
    }

    private String body(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new String(bytes, charset);
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

