package com.integrator.gateway.filter;

import com.integrator.gateway.model.RoutingRule;
import com.integrator.gateway.routing.RoutingRuleTargetResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class IntegratorRoutingGatewayFilterFactory extends AbstractGatewayFilterFactory<IntegratorRoutingGatewayFilterFactory.Config> {
    private final RoutingRuleTargetResolver routingRuleTargetResolver;
    private static final int ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
//    private static final int ORDER =  Integer.MAX_VALUE - 1;


    public IntegratorRoutingGatewayFilterFactory(RoutingRuleTargetResolver routingRuleTargetResolver) {
        super(Config.class);
        this.routingRuleTargetResolver = routingRuleTargetResolver;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            List<RoutingRule> routingRules = routingRules(exchange);
            if (routingRules.isEmpty()) {
                return chain.filter(exchange);
            }
            if (!hasBodyCondition(routingRules)) {
                Optional<URI> targetUri = routingRuleTargetResolver.resolve(routingRules, exchange, "");
                targetUri.ifPresent(uri -> setRequestUri(exchange, uri));
                return chain.filter(exchange);
            }
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                    .flatMap(dataBuffer -> routeWithBody(exchange, chain, routingRules, dataBuffer));
        }, ORDER);
    }

    private List<RoutingRule> routingRules(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return List.of();
        }
        Object routingRules = route.getMetadata().get("routingRules");
        if (!(routingRules instanceof List<?> rules)) {
            return List.of();
        }
        return rules.stream()
                .filter(RoutingRule.class::isInstance)
                .map(RoutingRule.class::cast)
                .toList();
    }

    private boolean hasBodyCondition(List<RoutingRule> routingRules) {
        return routingRules.stream()
                .filter(RoutingRule::isEnabled)
                .map(RoutingRule::getMatchConfig)
                .anyMatch(matchConfig -> matchConfig != null && matchConfig.contains("\"BODY\""));
    }

    private void setRequestUri(ServerWebExchange exchange, URI uri) {
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri);
    }

    private Mono<Void> routeWithBody(ServerWebExchange exchange, GatewayFilterChain chain, List<RoutingRule> routingRules, DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        String body = new String(bytes, StandardCharsets.UTF_8);
        Optional<URI> targetUri = routingRuleTargetResolver.resolve(routingRules, exchange, body);
        targetUri.ifPresent(uri -> setRequestUri(exchange, uri));
        ServerHttpRequest decoratedRequest = decorateRequest(exchange, bytes);
        return chain.filter(exchange.mutate().request(decoratedRequest).build());
    }

    private ServerHttpRequest decorateRequest(ServerWebExchange exchange, byte[] bytes) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }
        };
    }

    public static class Config {
    }
}
