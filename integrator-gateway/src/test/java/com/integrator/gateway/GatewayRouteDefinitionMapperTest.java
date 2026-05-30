package com.integrator.gateway;

import com.integrator.gateway.dto.Route;
import com.integrator.gateway.mapper.GatewayRouteDefinitionMapper;
import com.integrator.gateway.model.TransformType;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayRouteDefinitionMapperTest {
    private final GatewayRouteDefinitionMapper mapper = new GatewayRouteDefinitionMapper();

    @Test
    @Order(1)
    @DisplayName("Maps correctly when route is enabled")
    void addRouteSuccess() {
        Route route = defaultRoute();
        UUID routeId = route.getId();
        Optional<RouteDefinition> routeDefinitionOptional = mapper.toRouteDefinition(route);
        assertThat(routeDefinitionOptional).isPresent();

        RouteDefinition routeDefinition = routeDefinitionOptional.get();
        assertThat(routeDefinition.getId()).isEqualTo(routeId.toString());
        assertThat(routeDefinition.getUri()).hasToString("http://localhost:9000");
        assertThat(routeDefinition.getPredicates())
                .extracting(PredicateDefinition::getName)
                .containsExactly("Path", "Method");
        assertThat(routeDefinition.getPredicates().getFirst().getArgs()).containsValue("/agw/orders/**");
        assertThat(routeDefinition.getPredicates().get(1).getArgs()).containsValue("GET");
        assertThat(routeDefinition.getFilters().getFirst().getName()).isEqualTo("SetRequestUri");
        assertThat(routeDefinition.getMetadata())
                .containsEntry("integratorManaged", true)
                .containsEntry("routeId", routeId)
                .containsEntry("routeName", "orders");
    }

    @Test
    @Order(2)
    @DisplayName("Route is not added as not enabled")
    void addRouteFailureRouteIsNotEnabled() {
        Route route = defaultRoute();
        route.setEnabled(false);
        Optional<RouteDefinition> routeDefinition = mapper.toRouteDefinition(route);
        assertThat(routeDefinition).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Route is not added as target url is invalid")
    void addRouteFailureTargetUrlIsInvalid() {
        Route route = defaultRoute();
        route.setTargetUrl("https://");
        Optional<RouteDefinition> routeDefinition = mapper.toRouteDefinition(route);
        assertThat(routeDefinition).isEmpty();
    }

    private Route defaultRoute() {
        return Route.builder()
                .id(UUID.randomUUID())
                .name("orders")
                .pathPattern("/orders/**")
                .httpMethod(RequestMethod.GET)
                .targetUrl("http://localhost:9000")
                .transformType(TransformType.NONE)
                .enabled(true)
                .build();
    }
}
