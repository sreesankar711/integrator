package com.integrator.gateway;

import com.integrator.gateway.model.RoutingRule;
import com.integrator.gateway.routing.RoutingRuleConfigParser;
import com.integrator.gateway.routing.RoutingRuleMatcher;
import com.integrator.gateway.routing.RoutingRuleTargetResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingRuleTargetResolverTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final RoutingRuleTargetResolver resolver = new RoutingRuleTargetResolver(
            new RoutingRuleConfigParser(objectMapper),
            new RoutingRuleMatcher()
    );

    @Test
    @Order(1)
    @DisplayName("Lowest priority matching rule is selected")
    void resolveSuccessUsesLowestPriorityMatchingRule() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders").header("X-Client", "partner-d"));
        Optional<URI> uri = resolver.resolve(List.of(
                        rule(2, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"BODY\",\"key\":\"$.id\",\"equals\":\"2\"}]}",
                                "http://localhost:9000/order/2"),
                        rule(0, true, "{\"matchMode\":\"ANY\",\"conditions\":[{\"type\":\"BODY\",\"key\":\"$.id\",\"equals\":\"3\"},{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "http://localhost:9000/order/3")),
                exchange,
                "{\"id\":2}");
        assertThat(uri).hasValue(URI.create("http://localhost:9000/order/3"));
    }

    @Test
    @Order(2)
    @DisplayName("Disabled rules are skipped")
    void resolveSuccessSkipsDisabledRule() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders").header("X-Client", "partner-d"));
        Optional<URI> uri = resolver.resolve(List.of(
                        rule(0, false, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "http://localhost:9000/disabled"),
                        rule(1, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "http://localhost:9000/enabled")),
                exchange,
                "");
        assertThat(uri).hasValue(URI.create("http://localhost:9000/enabled"));
    }

    @Test
    @Order(3)
    @DisplayName("Invalid match config is skipped")
    void resolveSuccessSkipsInvalidMatchConfig() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders").header("X-Client", "partner-d"));
        Optional<URI> uri = resolver.resolve(List.of(
                        rule(0, true, "{invalid-json", "http://localhost:9000/invalid"),
                        rule(1, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "http://localhost:9000/valid")),
                exchange,
                "");
        assertThat(uri).hasValue(URI.create("http://localhost:9000/valid"));
    }

    @Test
    @Order(4)
    @DisplayName("Invalid override target is skipped")
    void resolveSuccessSkipsInvalidOverrideTarget() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders").header("X-Client", "partner-d"));
        Optional<URI> uri = resolver.resolve(List.of(
                        rule(0, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "https://"),
                        rule(1, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "localhost:9000/valid")),
                exchange,
                "");
        assertThat(uri).hasValue(URI.create("http://localhost:9000/valid"));
    }

    @Test
    @Order(5)
    @DisplayName("Empty result is returned when no rule matches")
    void resolveFailureNoMatch() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/orders").header("X-Client", "partner-a"));
        Optional<URI> uri = resolver.resolve(List.of(
                        rule(0, true, "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-d\"}]}",
                                "http://localhost:9000/order/3")),
                exchange,
                "");
        assertThat(uri).isEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("Empty result is returned when rules are missing")
    void resolveFailureMissingRules() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders"));
        Optional<URI> uri = resolver.resolve(null, exchange, "");
        assertThat(uri).isEmpty();
    }

    private RoutingRule rule(Integer priority, boolean enabled, String matchConfig, String overrideTargetUrl) {
        RoutingRule rule = new RoutingRule();
        rule.setId(UUID.randomUUID());
        rule.setPriority(priority);
        rule.setEnabled(enabled);
        rule.setMatchConfig(matchConfig);
        rule.setOverrideTargetUrl(overrideTargetUrl);
        return rule;
    }
}
