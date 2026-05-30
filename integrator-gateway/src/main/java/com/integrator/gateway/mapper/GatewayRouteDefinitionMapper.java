package com.integrator.gateway.mapper;

import com.integrator.gateway.dto.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GatewayRouteDefinitionMapper {
    private static final String PATH_PREDICATE = "Path";
    private static final String METHOD_PREDICATE = "Method";
    private static final String AGW_PREFIX = "/agw";
    private static final String SET_REQUEST_URI_FILTER = "SetRequestUri";
    private static final String SET_REQUEST_URI_TEMPLATE_ARG = "template";
    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://.*");


    public Optional<RouteDefinition> toRouteDefinition(Route route) {
        if (route == null) {
            return Optional.empty();
        }
        if (!route.isEnabled()) {
            log.warn("Skipping disabled route: routeId={}, routeName={}", route.getId(), route.getName());
            return Optional.empty();
        }
        Optional<URI> targetUri = normalizeTargetUrl(route.getTargetUrl());
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
        routeDefinition.setFilters(List.of(setRequestUriFilter(targetUri.get().toString())));
        routeDefinition.setUri(targetUri.get());
        routeDefinition.setEnabled(true);
        routeDefinition.setMetadata(metadata(route));
        return Optional.of(routeDefinition);
    }

    private Optional<URI> normalizeTargetUrl(String targetUrl) {
        if (!StringUtils.hasText(targetUrl)) {
            return Optional.empty();
        }
        String normalizedTargetUrl = targetUrl.trim();
        if (!HAS_SCHEME.matcher(normalizedTargetUrl).matches()) {
            normalizedTargetUrl = "http://" + normalizedTargetUrl;
        }
        try {
            URI uri = URI.create(normalizedTargetUrl);
            if (!uri.isAbsolute() || uri.getHost() == null ||
                    !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
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

    private Map<String, Object> metadata(Route route) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("integratorManaged", true);
        metadata.put("routeId", route.getId());
        metadata.put("routeName", route.getName());
        metadata.put("transformType", route.getTransformType());
        metadata.put("fieldMappingConfig", route.getFieldMappingConfig());
        metadata.put("snippetId", route.getSnippetId());
        return metadata;
    }
}
