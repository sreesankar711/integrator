package com.integrator.gateway.mapper;

import com.integrator.gateway.dto.Route;
import com.integrator.gateway.utils.NormalizeTargetUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class GatewayRouteDefinitionMapper {
    private static final String PATH_PREDICATE = "Path";
    private static final String METHOD_PREDICATE = "Method";
    private static final String AGW_PREFIX = "/agw";
    private static final String SET_REQUEST_URI_FILTER = "SetRequestUri";
    private static final String SET_REQUEST_URI_TEMPLATE_ARG = "template";
    private static final String INTEGRATOR_ROUTING_FILTER = "IntegratorRouting";
    private static final String REQUEST_RATE_LIMITER_FILTER = "RequestRateLimiter";
    private static final String KEY_RESOLVER_ARG = "keyResolver";
    private static final String REDIS_RATE_LIMITER_REPLENISH_RATE_ARG = "redis-rate-limiter.replenishRate";
    private static final String REDIS_RATE_LIMITER_BURST_CAPACITY_ARG = "redis-rate-limiter.burstCapacity";
    private static final String REDIS_RATE_LIMITER_REQUESTED_TOKENS_ARG = "redis-rate-limiter.requestedTokens";

    public Optional<RouteDefinition> toRouteDefinition(Route route) {
        if (route == null) {
            return Optional.empty();
        }
        if (!route.isEnabled()) {
            log.warn("Skipping disabled route: routeId={}, routeName={}", route.getId(), route.getName());
            return Optional.empty();
        }
        Optional<URI> targetUri = NormalizeTargetUrl.getURL(route.getTargetUrl());
        if (targetUri.isEmpty()) {
            log.warn("Skipping gateway route with invalid target URL: routeId={}, targetUrl={}",
                    route.getId(), route.getTargetUrl());
            return Optional.empty();
        }
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(route.getId().toString());
        routeDefinition.setPredicates(List.of(
                predicate(PATH_PREDICATE, prefixPathPattern(route.getPathPattern())),
                predicate(METHOD_PREDICATE, route.getHttpMethod().name())
        ));
        routeDefinition.setFilters(filterDefinitions(route, targetUri.get().toString()));
        routeDefinition.setOrder(routeOrder(route.getPathPattern()));
        routeDefinition.setUri(targetUri.get());
        routeDefinition.setEnabled(true);
        routeDefinition.setMetadata(metadata(route));
        return Optional.of(routeDefinition);
    }

    private String prefixPathPattern(String pathPattern) {
        String normalizedPathPattern = pathPattern.trim();

        if (normalizedPathPattern.equals(AGW_PREFIX)
                || normalizedPathPattern.startsWith(AGW_PREFIX + "/")) {
            return normalizedPathPattern;
        }
        if (normalizedPathPattern.equals(AGW_PREFIX.substring(1))
                || normalizedPathPattern.startsWith(AGW_PREFIX.substring(1) + "/")) {
            return "/" + normalizedPathPattern;
        }
        if (normalizedPathPattern.startsWith("/")) {
            return AGW_PREFIX + normalizedPathPattern;
        }
        return AGW_PREFIX + "/" + pathPattern;
    }

    private PredicateDefinition predicate(String name, String value) {
        PredicateDefinition predicateDefinition = new PredicateDefinition();
        predicateDefinition.setName(name);
        predicateDefinition.addArg(NameUtils.generateName(0), value);
        return predicateDefinition;
    }

    private FilterDefinition setRequestUriFilter(String targetUrl) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(SET_REQUEST_URI_FILTER);
        filterDefinition.addArg(SET_REQUEST_URI_TEMPLATE_ARG, targetUrl);
        return filterDefinition;
    }

    private FilterDefinition integratorRoutingFilter() {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(INTEGRATOR_ROUTING_FILTER);
        return filterDefinition;
    }

    private List<FilterDefinition> filterDefinitions(Route route, String targetUrl) {
        List<FilterDefinition> filters = new ArrayList<>();
        if (route.isRateLimitEnabled()) {
            filters.add(requestRateLimiterFilter(route));
        }
        filters.add(setRequestUriFilter(targetUrl));
        if (route.getRoutingRules() != null && !route.getRoutingRules().isEmpty()) {
            filters.add(integratorRoutingFilter());
        }
        return filters;
    }

    private int routeOrder(String pathPattern) {
        if (pathPattern.contains("**")) {
            return 100;
        }
        return 0;
    }

    private Map<String, Object> metadata(Route route) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("integratorManaged", true);
        metadata.put("routeId", route.getId());
        metadata.put("routeName", route.getName());
        metadata.put("transformType", route.getTransformType());
        metadata.put("fieldMappingConfig", route.getFieldMappingConfig());
        metadata.put("snippetId", route.getSnippetId());
        metadata.put("routingRules", route.getRoutingRules());
        metadata.put("rateLimitEnabled", route.isRateLimitEnabled());
        metadata.put("rateLimitReplenishRate", route.getRateLimitReplenishRate());
        metadata.put("rateLimitBurstCapacity", route.getRateLimitBurstCapacity());
        metadata.put("rateLimitRequestedTokens", route.getRateLimitRequestedTokens());
        return metadata;
    }

    private FilterDefinition requestRateLimiterFilter(Route route) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(REQUEST_RATE_LIMITER_FILTER);
        filterDefinition.addArg(KEY_RESOLVER_ARG, "#{@routeRateLimitKeyResolver}");
        filterDefinition.addArg(REDIS_RATE_LIMITER_REPLENISH_RATE_ARG, route.getRateLimitReplenishRate().toString());
        filterDefinition.addArg(REDIS_RATE_LIMITER_BURST_CAPACITY_ARG, route.getRateLimitBurstCapacity().toString());
        filterDefinition.addArg(REDIS_RATE_LIMITER_REQUESTED_TOKENS_ARG, route.getRateLimitRequestedTokens().toString());
        return filterDefinition;
    }
}
