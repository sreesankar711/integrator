package com.integrator.gateway.routing;

import com.integrator.gateway.routing.model.RouteMatchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingRuleConfigParser {
    private final ObjectMapper objectMapper;

    public Optional<RouteMatchConfig> parse(String matchConfig) {
        if (matchConfig == null || matchConfig.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(matchConfig, RouteMatchConfig.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
