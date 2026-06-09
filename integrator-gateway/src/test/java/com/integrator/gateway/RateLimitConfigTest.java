package com.integrator.gateway;

import com.integrator.gateway.config.RateLimitConfig;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.URI;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RateLimitConfigTest {
    private final KeyResolver keyResolver = new RateLimitConfig().routeRateLimitKeyResolver();

    @Test
    @Order(1)
    @DisplayName("Uses gateway route id as rate limit key")
    void rateLimitKeySuccessRouteId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/agw/orders/123")
        );
        Route route = Route.async()
                .id("route-123")
                .uri(URI.create("http://localhost:9000"))
                .predicate(serverWebExchange -> true)
                .build();
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("route:route-123")
                .verifyComplete();
    }

    @Test
    @Order(2)
    @DisplayName("Uses fallback key when gateway route is missing")
    void rateLimitKeySuccessRouteMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/agw/orders/123")
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("route:unknown")
                .verifyComplete();
    }
}
