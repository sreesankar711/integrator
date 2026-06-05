package com.integrator.gateway;

import com.integrator.gateway.filter.IntegratorRoutingGatewayFilterFactory;
import com.integrator.gateway.model.RoutingRule;
import com.integrator.gateway.routing.RoutingRuleConfigParser;
import com.integrator.gateway.routing.RoutingRuleMatcher;
import com.integrator.gateway.routing.RoutingRuleTargetResolver;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegratorRoutingGatewayFilterFactoryTest {
    private final IntegratorRoutingGatewayFilterFactory filterFactory = new IntegratorRoutingGatewayFilterFactory(
            new RoutingRuleTargetResolver(
                    new RoutingRuleConfigParser(JsonMapper.builder().build()),
                    new RoutingRuleMatcher()
            )
    );

    @Test
    @Order(1)
    @DisplayName("Request continues unchanged when route has no routing rules")
    void filterSuccessNoRoutingRules() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute(List.of()));
        filter().filter(exchange, chainExchange -> Mono.empty()).block();
        assertThat((Object) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)).isNull();
    }

    @Test
    @Order(2)
    @DisplayName("Request continues unchanged when gateway route is missing")
    void filterSuccessGatewayRouteMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        filter().filter(exchange, chainExchange -> Mono.empty()).block();
        assertThat((Object) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)).isNull();
    }

    @Test
    @Order(3)
    @DisplayName("Request continues unchanged when routing rule metadata is not a list")
    void filterSuccessRoutingRuleMetadataIsNotList() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute("not-rules"));
        filter().filter(exchange, chainExchange -> Mono.empty()).block();
        assertThat((Object) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)).isNull();
    }

    @Test
    @Order(4)
    @DisplayName("Header routing rule overrides target URI")
    void filterSuccessIgnoresNonRoutingRuleMetadataItems() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/orders").header("X-Client", "partner-d")
        );
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute(List.of(
                "not-a-routing-rule",
                rule("{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                        "http://localhost:9000/order/3")
        )));
        filter().filter(exchange, chainExchange -> Mono.empty()).block();
        assertThat((URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
                .isEqualTo(URI.create("http://localhost:9000/order/3"));
    }

    @Test
    @Order(5)
    @DisplayName("Body routing rule overrides target URI and keeps request body readable")
    void filterSuccessBodyRuleOverridesTargetUriAndReplaysBody() {
        String requestBody = "{\"id\":2}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/orders")
                        .header("Content-Type", "application/json")
                        .body(requestBody)
        );
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, gatewayRoute(List.of(
                rule("{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"BODY\",\"key\":\"$.id\",\"equals\":\"2\"}]}",
                        "http://localhost:9000/order/2")
        )));
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        filter().filter(exchange, chainExchange -> DataBufferUtils.join(chainExchange.getRequest().getBody())
                .doOnNext(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    forwardedBody.set(new String(bytes, StandardCharsets.UTF_8));
                })
                .then()).block();
        assertThat((URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
                .isEqualTo(URI.create("http://localhost:9000/order/2"));
        assertThat(forwardedBody.get()).isEqualTo(requestBody);
    }

    private GatewayFilter filter() {
        return filterFactory.apply(new IntegratorRoutingGatewayFilterFactory.Config());
    }

    private Route gatewayRoute(List<RoutingRule> routingRules) {
        return gatewayRoute((Object) routingRules);
    }

    private Route gatewayRoute(Object routingRules) {
        return Route.async()
                .id("orders")
                .uri("http://localhost:9000/order/1")
                .predicate(exchange -> true)
                .metadata("routingRules", routingRules)
                .build();
    }

    private RoutingRule rule(String matchConfig, String overrideTargetUrl) {
        RoutingRule rule = new RoutingRule();
        rule.setId(UUID.randomUUID());
        rule.setEnabled(true);
        rule.setPriority(0);
        rule.setMatchConfig(matchConfig);
        rule.setOverrideTargetUrl(overrideTargetUrl);
        return rule;
    }
}
