package com.integrator.gateway;

import com.integrator.common.event.RouteEvent;
import com.integrator.common.event.RouteEventType;
import com.integrator.gateway.client.RouteServiceClient;
import com.integrator.gateway.dto.Route;
import com.integrator.gateway.event.RouteEventConsumerReadiness;
import com.integrator.gateway.mapper.GatewayRouteDefinitionMapper;
import com.integrator.gateway.model.TransformType;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import com.integrator.gateway.startup.GatewayRouteRegistrationStartup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RouteEventKafkaIntegrationTest extends AbstractContainerBaseTest{
    @MockitoBean
    private RouteServiceClient routeServiceClient;
    @MockitoBean
    private GatewayRouteRegistrationStartup gatewayRouteRegistrationStartup;
    @Autowired
    private RouteDefinitionRepository routeDefinitionRepository;
    @Autowired
    private RouteEventConsumerReadiness routeEventConsumerReadiness;

    @Test
    @Order(1)
    @DisplayName("Route is registered when create event is consumed from Kafka")
    void routeEventKafkaSuccessRouteIsCreated() throws Exception {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        when(routeServiceClient.getRoute(routeId)).thenReturn(route);
        routeEventConsumerReadiness.awaitReady();
        kafkaTemplate.send("route.events", routeId.toString(), routeEvent(RouteEventType.CREATED, routeId))
                .get(10, TimeUnit.SECONDS);
        RouteDefinition routeDefinition = waitForRouteDefinition(routeId);
        assertThat(routeDefinition).isNotNull();
        assertThat(routeDefinition.getId()).isEqualTo(routeId.toString());
        assertThat(routeDefinition.getUri()).hasToString("http://localhost:9000");
    }

    @Test
    @Order(2)
    @DisplayName("Route is updated when update event is consumed from Kafka")
    void routeEventKafkaSuccessRouteIsUpdated() throws Exception {
        UUID routeId = UUID.randomUUID();
        Route oldRoute = defaultRoute(routeId);
        RouteDefinition oldRouteDefinition = new GatewayRouteDefinitionMapper()
                .toRouteDefinition(oldRoute)
                .orElseThrow();
        routeDefinitionRepository.save(Mono.just(oldRouteDefinition)).block();
        Route updatedRoute = defaultRoute(routeId);
        updatedRoute.setTargetUrl("http://localhost:9001");
        when(routeServiceClient.getRoute(routeId)).thenReturn(updatedRoute);
        routeEventConsumerReadiness.awaitReady();
        kafkaTemplate.send("route.events", routeId.toString(), routeEvent(RouteEventType.UPDATED, routeId))
                .get(10, TimeUnit.SECONDS);
        RouteDefinition routeDefinition = waitForRouteDefinitionUri(routeId, "http://localhost:9001");
        assertThat(routeDefinition).isNotNull();
        assertThat(routeDefinition.getId()).isEqualTo(routeId.toString());
        assertThat(routeDefinition.getUri()).hasToString("http://localhost:9001");
    }

    @Test
    @Order(3)
    @DisplayName("Route is removed when delete event is consumed from Kafka")
    void routeEventKafkaSuccessRouteIsDeleted() throws Exception {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        RouteDefinition routeDefinition = new GatewayRouteDefinitionMapper()
                .toRouteDefinition(route)
                .orElseThrow();
        routeDefinitionRepository.save(Mono.just(routeDefinition)).block();
        routeEventConsumerReadiness.awaitReady();
        kafkaTemplate.send("route.events", routeId.toString(), routeEvent(RouteEventType.DELETED, routeId))
                .get(10, TimeUnit.SECONDS);
        assertThat(waitForRouteDefinitionToBeDeleted(routeId)).isTrue();
    }

    private RouteDefinition waitForRouteDefinition(UUID routeId) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<RouteDefinition> routeDefinition = findRouteDefinition(routeId);
            if (routeDefinition.isPresent()) {
                return routeDefinition.get();
            }
        }
        return null;
    }

    private RouteDefinition waitForRouteDefinitionUri(UUID routeId, String uri) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<RouteDefinition> routeDefinition = findRouteDefinition(routeId)
                    .filter(definition -> uri.equals(definition.getUri().toString()));
            if (routeDefinition.isPresent()) {
                return routeDefinition.get();
            }
        }
        return null;
    }

    private boolean waitForRouteDefinitionToBeDeleted(UUID routeId){
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (findRouteDefinition(routeId).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Optional<RouteDefinition> findRouteDefinition(UUID routeId) {
        return routeDefinitionRepository.getRouteDefinitions()
                .collectList()
                .block()
                .stream()
                .filter(definition -> definition.getId().equals(routeId.toString()))
                .findFirst();
    }

    private RouteEvent routeEvent(RouteEventType routeEventType, UUID routeId) {
        return RouteEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .routeEventType(routeEventType)
                .routeId(routeId)
                .build();
    }

    private Route defaultRoute(UUID routeId) {
        return Route.builder()
                .id(routeId)
                .name("orders")
                .pathPattern("/orders/**")
                .httpMethod(RequestMethod.GET)
                .targetUrl("http://localhost:9000")
                .transformType(TransformType.NONE)
                .enabled(true)
                .build();
    }
}
