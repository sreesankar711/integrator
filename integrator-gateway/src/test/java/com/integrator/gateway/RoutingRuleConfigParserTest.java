package com.integrator.gateway;

import com.integrator.gateway.routing.RoutingRuleConfigParser;
import com.integrator.gateway.routing.model.RouteConditionType;
import com.integrator.gateway.routing.model.RouteMatchConfig;
import com.integrator.gateway.routing.model.RouteMatchMode;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoutingRuleConfigParserTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final RoutingRuleConfigParser parser = new RoutingRuleConfigParser(objectMapper);

    @Test
    @Order(1)
    @DisplayName("Valid routing rule config is parsed")
    void parseSuccess() {
        Optional<RouteMatchConfig> config = parser.parse(
                "{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"BODY\",\"key\":\"$.id\",\"equals\":\"2\"}]}"
        );
        assertThat(config).isPresent();
        assertThat(config.get().getMatchMode()).isEqualTo(RouteMatchMode.ALL);
        assertThat(config.get().getConditions()).hasSize(1);
        assertThat(config.get().getConditions().getFirst().getType()).isEqualTo(RouteConditionType.BODY);
    }

    @Test
    @Order(2)
    @DisplayName("Blank routing rule config returns empty")
    void parseFailureBlankConfig() {
        assertThat(parser.parse(" ")).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Invalid routing rule config returns empty")
    void parseFailureInvalidConfig() {
        assertThat(parser.parse("{invalid-json")).isEmpty();
    }
}
