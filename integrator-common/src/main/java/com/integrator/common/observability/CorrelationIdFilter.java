package com.integrator.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,  HttpServletResponse response, FilterChain filterChain)
                                    throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        String xCorrelationId = request.getHeader(CorrelationConstants.CORRELATION_ID_HEADER);
        MDC.put(CorrelationConstants.MDC_CORRELATION_ID_KEY, correlationId);
        MDC.put(CorrelationConstants.CORRELATION_ID_HEADER, xCorrelationId);
        response.setHeader(CorrelationConstants.CORRELATION_ID_HEADER, xCorrelationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationConstants.MDC_CORRELATION_ID_KEY);
            MDC.remove(CorrelationConstants.CORRELATION_ID_HEADER);
        }
    }
}