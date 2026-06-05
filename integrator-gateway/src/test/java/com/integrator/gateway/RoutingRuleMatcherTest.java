package com.integrator.gateway;

import com.integrator.gateway.routing.RoutingRuleMatcher;
import com.integrator.gateway.routing.model.Condition;
import com.integrator.gateway.routing.model.RouteConditionType;
import com.integrator.gateway.routing.model.RouteMatchConfig;
import com.integrator.gateway.routing.model.RouteMatchMode;
import org.junit.jupiter.api.*;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoutingRuleMatcherTest {
    private final RoutingRuleMatcher routingRuleMatcher = new RoutingRuleMatcher();

    @Test
    @Order(1)
    @DisplayName("Header condition matches request header")
    void matchesSuccessHeaderCondition() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").header("X-Client", "partner-d"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.HEADER, "X-Client", "partner-d")), exchange, "")
        ).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Query condition matches request query parameter")
    void matchesSuccessQueryCondition() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders?region=EU"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.QUERY, "region", "EU")), exchange, "")
        ).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Body condition matches JSONPath value")
    void matchesSuccessBodyCondition() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.BODY, "$.order.id", "2")), exchange, "{\"order\":{\"id\":2}}")
        ).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("All mode requires every condition to match")
    void matchesFailureAllConditionDoesNotMatch() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").header("X-Client", "partner-d"));
        assertThat(routingRuleMatcher.matches(
                config(
                        RouteMatchMode.ALL,
                        condition(RouteConditionType.HEADER, "X-Client", "partner-d"),
                        condition(RouteConditionType.BODY, "$.id", "3")
                ),
                exchange,
                "{\"id\":2}")
        ).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("Any mode matches when one condition matches")
    void matchesSuccessAnyConditionMatches() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").header("X-Client", "partner-d"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ANY,
                        condition(RouteConditionType.BODY, "$.id", "3"),
                        condition(RouteConditionType.HEADER, "X-Client", "partner-d")
                ),
                exchange,
                "{\"id\":2}")
        ).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("None mode never matches")
    void matchesFailureNoneMode() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").header("X-Client", "partner-d"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.NONE, condition(RouteConditionType.HEADER, "X-Client", "partner-d")), exchange, "")
        ).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("Invalid body JSONPath does not match")
    void matchesFailureInvalidBodyPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.BODY, "$.missing.id", "2")), exchange, "{\"id\":2}")
        ).isFalse();
    }

    @Test
    @Order(8)
    @DisplayName("Condition without type does not match")
    void matchesFailureConditionTypeMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        assertThat(routingRuleMatcher.matches(config(RouteMatchMode.ALL, new Condition()), exchange, "")).isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("Header condition without key does not match")
    void matchesFailureHeaderKeyMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").header("X-Client", "partner-d"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.HEADER, null, "partner-d")), exchange, "")
        ).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("Query condition without expected value does not match")
    void matchesFailureQueryExpectedValueMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders?id=2"));
        assertThat(routingRuleMatcher.matches(
                config(RouteMatchMode.ALL, condition(RouteConditionType.QUERY, "id", "3")), exchange, "")
        ).isFalse();
    }

    private RouteMatchConfig config(RouteMatchMode matchMode, Condition... conditions) {
        RouteMatchConfig config = new RouteMatchConfig();
        config.setMatchMode(matchMode);
        config.setConditions(List.of(conditions));
        return config;
    }

    private Condition condition(RouteConditionType type, String key, String equals) {
        Condition condition = new Condition();
        condition.setType(type);
        condition.setKey(key);
        condition.setEquals(equals);
        return condition;
    }
}
