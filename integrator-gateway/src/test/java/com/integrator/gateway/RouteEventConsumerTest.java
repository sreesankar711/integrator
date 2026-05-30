package com.integrator.gateway;

import com.integrator.common.event.RouteEvent;
import com.integrator.common.event.RouteEventType;
import com.integrator.gateway.config.GatewayProperties;
import com.integrator.gateway.event.RouteEventConsumer;
import com.integrator.gateway.event.RouteEventConsumerReadiness;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RouteEventConsumerTest {
    private final GatewayRouteRegistrationService routeRegistrationService = mock(GatewayRouteRegistrationService.class);
    private final RouteEventConsumerReadiness routeEventConsumerReadiness = mock(RouteEventConsumerReadiness.class);
    private final RouteEventConsumer routeEventConsumer = new RouteEventConsumer(routeRegistrationService, gatewayProperties(), routeEventConsumerReadiness);

    @Test
    @Order(1)
    @DisplayName("Route is refreshed when created event is received")
    void routeEventSuccessRouteIsCreated() {
        UUID routeId = UUID.randomUUID();
        routeEventConsumer.onRouteEvent(routeEvent(RouteEventType.CREATED, routeId));
        verify(routeRegistrationService).createOrUpdateRoute(routeId);
        verifyNoMoreInteractions(routeRegistrationService);
    }

    @Test
    @Order(2)
    @DisplayName("Route is refreshed when updated event is received")
    void routeEventSuccessRouteIsUpdated() {
        UUID routeId = UUID.randomUUID();
        routeEventConsumer.onRouteEvent(routeEvent(RouteEventType.UPDATED, routeId));
        verify(routeRegistrationService).createOrUpdateRoute(routeId);
        verifyNoMoreInteractions(routeRegistrationService);
    }

    @Test
    @Order(3)
    @DisplayName("Route is deleted when deleted event is received")
    void routeEventSuccessRouteIsDeleted() {
        UUID routeId = UUID.randomUUID();
        routeEventConsumer.onRouteEvent(routeEvent(RouteEventType.DELETED, routeId));
        verify(routeRegistrationService).deleteRoute(routeId);
        verifyNoMoreInteractions(routeRegistrationService);
    }

    private GatewayProperties gatewayProperties() {
        GatewayProperties gatewayProperties = new GatewayProperties();
        gatewayProperties.setRouteEventsTopic("route.events");
        return gatewayProperties;
    }

    private RouteEvent routeEvent(RouteEventType routeEventType, UUID routeId) {
        return RouteEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .routeEventType(routeEventType)
                .routeId(routeId)
                .build();
    }
}
