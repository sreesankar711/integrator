package com.integrator.gateway.routing;

import com.integrator.gateway.model.RoutingRule;
import com.integrator.gateway.routing.model.RouteMatchConfig;
import com.integrator.gateway.utils.NormalizeTargetUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingRuleTargetResolver {
    private final RoutingRuleConfigParser routingRuleConfigParser;
    private final RoutingRuleMatcher routingRuleMatcher;

    public Optional<URI> resolve(List<RoutingRule> routingRules, ServerWebExchange exchange, String body) {
        if (routingRules == null || routingRules.isEmpty()) {
            return Optional.empty();
        }
        return routingRules.stream()
                .filter(RoutingRule::isEnabled)
                .sorted(Comparator.comparing(routingRule -> routingRule.getPriority() == null ? Integer.MAX_VALUE : routingRule.getPriority()))
                .map(routingRule -> resolveRule(routingRule, exchange, body))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<URI> resolveRule(RoutingRule routingRule, ServerWebExchange exchange, String body) {
        Optional<RouteMatchConfig> routeMatchConfig = routingRuleConfigParser.parse(routingRule.getMatchConfig());
        if (routeMatchConfig.isEmpty() || !routingRuleMatcher.matches(routeMatchConfig.get(), exchange, body)) {
            return Optional.empty();
        }
        Optional<URI> overrideTargetUri = NormalizeTargetUrl.getURL(routingRule.getOverrideTargetUrl());
        if (overrideTargetUri.isEmpty()) {
            log.warn("Skipping matched routingRule with invalid override target URL: ruleId={}, targetUrl={}",
                    routingRule.getId(), routingRule.getOverrideTargetUrl());
        }
        return overrideTargetUri;
    }
}
