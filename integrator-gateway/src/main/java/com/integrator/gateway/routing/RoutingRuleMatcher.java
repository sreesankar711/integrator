package com.integrator.gateway.routing;

import com.integrator.gateway.routing.model.Condition;
import com.integrator.gateway.routing.model.RouteMatchConfig;
import com.integrator.gateway.routing.model.RouteMatchMode;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Component
public class RoutingRuleMatcher {

    public boolean matches(RouteMatchConfig routeMatchConfig, ServerWebExchange exchange, String body) {
        if (routeMatchConfig == null || routeMatchConfig.getMatchMode() == RouteMatchMode.NONE) {
            return false;
        }
        List<Condition> conditions = routeMatchConfig.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        if (routeMatchConfig.getMatchMode() == RouteMatchMode.ANY) {
            return conditions.stream().anyMatch(condition -> conditionMatches(condition, exchange, body));
        }
        return conditions.stream().allMatch(condition -> conditionMatches(condition, exchange, body));
    }

    private boolean conditionMatches(Condition routeMatchCondition, ServerWebExchange exchange, String body) {
        if (routeMatchCondition == null || routeMatchCondition.getType() == null) {
            return false;
        }
        return switch (routeMatchCondition.getType()) {
            case HEADER -> headerMatches(routeMatchCondition, exchange);
            case QUERY -> queryMatches(routeMatchCondition, exchange);
            case BODY -> bodyMatches(routeMatchCondition, body);
        };
    }

    private boolean headerMatches(Condition routeMatchCondition, ServerWebExchange exchange) {
        if (routeMatchCondition.getKey() == null || routeMatchCondition.getEquals() == null) {
            return false;
        }
        String headerValue = exchange.getRequest().getHeaders().getFirst(routeMatchCondition.getKey());
        return routeMatchCondition.getEquals().equals(headerValue);
    }

    private boolean queryMatches(Condition routeMatchCondition, ServerWebExchange exchange) {
        if (routeMatchCondition.getKey() == null || routeMatchCondition.getEquals() == null) {
            return false;
        }
        String queryValue = exchange.getRequest().getQueryParams().getFirst(routeMatchCondition.getKey());
        return routeMatchCondition.getEquals().equals(queryValue);
    }

    private boolean bodyMatches(Condition condition, String body) {
        if (condition.getKey() == null || condition.getEquals() == null || body == null) {
            return false;
        }
        try {
            Object bodyValue = JsonPath.read(body, condition.getKey());
            return condition.getEquals().equals(String.valueOf(bodyValue));
        } catch (Exception ex) {
            return false;
        }
    }
}
