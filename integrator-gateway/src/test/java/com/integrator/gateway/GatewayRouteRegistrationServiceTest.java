package com.integrator.gateway;

import com.integrator.gateway.client.RouteServiceClient;
import com.integrator.gateway.dto.Route;
import com.integrator.gateway.mapper.GatewayRouteDefinitionMapper;
import com.integrator.gateway.model.TransformType;
import com.integrator.gateway.service.GatewayRouteRegistrationService;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayRouteRegistrationServiceTest {
    private final RouteServiceClient routeServiceClient = mock(RouteServiceClient.class);
    private final GatewayRouteDefinitionMapper routeDefinitionMapper = mock(GatewayRouteDefinitionMapper.class);
    private final RouteDefinitionRepository routeDefinitionRepository = mock(RouteDefinitionRepository.class);
    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

    private final GatewayRouteRegistrationService gatewayRouteRegistrationService =
            new GatewayRouteRegistrationService(
                    routeServiceClient,
                    routeDefinitionMapper,
                    routeDefinitionRepository,
                    applicationEventPublisher
            );

    @Test
    @Order(1)
    @DisplayName("Routes are registered from Route Service")
    void registerRoutesSuccess() {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        RouteDefinition routeDefinition = routeDefinition(routeId);
        when(routeServiceClient.getAllRoutes()).thenReturn(List.of(route));
        when(routeDefinitionMapper.toRouteDefinition(route)).thenReturn(Optional.of(routeDefinition));
        when(routeDefinitionRepository.save(any())).thenReturn(Mono.empty());
        gatewayRouteRegistrationService.registerRoutes();
        verify(routeServiceClient).getAllRoutes();
        verify(routeDefinitionMapper).toRouteDefinition(route);
        verify(routeDefinitionRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    @Order(2)
    @DisplayName("Route is skipped when mapper returns empty")
    void registerRoutesSuccessRouteIsSkipped() {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        when(routeServiceClient.getAllRoutes()).thenReturn(List.of(route));
        when(routeDefinitionMapper.toRouteDefinition(route)).thenReturn(Optional.empty());
        gatewayRouteRegistrationService.registerRoutes();
        verify(routeServiceClient).getAllRoutes();
        verify(routeDefinitionMapper).toRouteDefinition(route);
        verify(routeDefinitionRepository, never()).save(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    @Order(3)
    @DisplayName("Startup registration fails when Route Service is unavailable")
    void registerRoutesFailureRouteServiceUnavailable() {
        when(routeServiceClient.getAllRoutes()).thenThrow(new RuntimeException("down"));
        assertThatThrownBy(gatewayRouteRegistrationService::registerRoutes)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("down");
        verifyNoInteractions(routeDefinitionMapper);
        verifyNoInteractions(routeDefinitionRepository);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @Order(4)
    @DisplayName("Refresh replaces existing route definitions")
    void refreshRoutesSuccessReplacesExistingRoutes() {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        RouteDefinition routeDefinition = routeDefinition(routeId);
        when(routeServiceClient.getAllRoutes()).thenReturn(List.of(route));
        when(routeDefinitionMapper.toRouteDefinition(route)).thenReturn(Optional.of(routeDefinition));
        when(routeDefinitionRepository.delete(any())).thenReturn(Mono.empty());
        when(routeDefinitionRepository.save(any())).thenReturn(Mono.empty());
        gatewayRouteRegistrationService.refreshRoutes();
        verify(routeServiceClient).getAllRoutes();
        verify(routeDefinitionRepository).delete(any());
        verify(routeDefinitionMapper).toRouteDefinition(route);
        verify(routeDefinitionRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    @Order(5)
    @DisplayName("Scheduled refresh is skipped when Route Service is unavailable")
    void refreshRoutesFailureRouteServiceUnavailable() {
        when(routeServiceClient.getAllRoutes()).thenThrow(new RuntimeException("down"));
        gatewayRouteRegistrationService.refreshRoutes();
        verify(routeServiceClient).getAllRoutes();
        verifyNoInteractions(routeDefinitionMapper);
        verifyNoInteractions(routeDefinitionRepository);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @Order(6)
    @DisplayName("Route is created or updated when Route Service returns route")
    void createOrUpdateRouteSuccess() {
        UUID routeId = UUID.randomUUID();
        Route route = defaultRoute(routeId);
        RouteDefinition routeDefinition = routeDefinition(routeId);
        when(routeServiceClient.getRoute(routeId)).thenReturn(route);
        when(routeDefinitionRepository.delete(any())).thenReturn(Mono.empty());
        when(routeDefinitionMapper.toRouteDefinition(route)).thenReturn(Optional.of(routeDefinition));
        when(routeDefinitionRepository.save(any())).thenReturn(Mono.empty());
        gatewayRouteRegistrationService.createOrUpdateRoute(routeId);
        verify(routeServiceClient).getRoute(routeId);
        verify(routeDefinitionRepository).delete(any());
        verify(routeDefinitionMapper).toRouteDefinition(route);
        verify(routeDefinitionRepository).save(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    @Order(7)
    @DisplayName("Route is only deleted when Route Service returns no route")
    void createOrUpdateRouteSuccessRouteIsMissing() {
        UUID routeId = UUID.randomUUID();
        when(routeServiceClient.getRoute(routeId)).thenReturn(null);
        when(routeDefinitionRepository.delete(any())).thenReturn(Mono.empty());
        gatewayRouteRegistrationService.createOrUpdateRoute(routeId);
        verify(routeServiceClient).getRoute(routeId);
        verify(routeDefinitionRepository).delete(any());
        verifyNoInteractions(routeDefinitionMapper);
        verify(routeDefinitionRepository, never()).save(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    @Order(8)
    @DisplayName("Route is deleted")
    void deleteRouteSuccess() {
        UUID routeId = UUID.randomUUID();
        when(routeDefinitionRepository.delete(any())).thenReturn(Mono.empty());
        gatewayRouteRegistrationService.deleteRoute(routeId);
        verify(routeDefinitionRepository).delete(any());
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
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

    private RouteDefinition routeDefinition(UUID routeId) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(routeId.toString());
        routeDefinition.setUri(URI.create("http://localhost:9000"));
        return routeDefinition;
    }
}
