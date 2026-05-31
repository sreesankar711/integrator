package com.integrator.gateway.service;

import com.integrator.gateway.client.RouteServiceClient;
import com.integrator.gateway.dto.Route;
import com.integrator.gateway.mapper.GatewayRouteDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayRouteRegistrationService {
    private final RouteServiceClient routeServiceClient;
    private final GatewayRouteDefinitionMapper routeDefinitionMapper;
    private final RouteDefinitionRepository routeDefinitionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void registerRoutes() {
        List<Route> routes = routeServiceClient.getAllRoutes();
        applyRoutes(routes, false);
    }

    public void refreshRoutes() {
        List<Route> routes;
        try {
            routes = routeServiceClient.getAllRoutes();
        } catch (Exception ex) {
            log.warn("Gateway route refresh skipped. Route Service is unavailable. {}", ex.getLocalizedMessage());
            return;
        }
        applyRoutes(routes, true);
    }

    private void applyRoutes(List<Route> routes, boolean isRefresh) {
        int registeredRoutes = 0;
        int skippedRoutes = 0;
        for (Route route : routes) {
            if (isRefresh) {
                routeDefinitionRepository.delete(Mono.just(route.getId().toString()))
                        .onErrorResume(error -> Mono.empty())
                        .block();
            }
            Optional<RouteDefinition> routeDefinition = routeDefinitionMapper.toRouteDefinition(route);
            if (routeDefinition.isEmpty()) {
                skippedRoutes++;
                continue;
            }
            routeDefinitionRepository.save(Mono.just(routeDefinition.get())).block();
            registeredRoutes++;
        }
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Gateway route registration completed. fetched={}, registered={}, skipped={}",
                routes.size(), registeredRoutes, skippedRoutes);
    }

    public void createOrUpdateRoute(UUID routeId) {
        Route route = routeServiceClient.getRoute(routeId);
        routeDefinitionRepository.delete(Mono.just(routeId.toString()))
                .onErrorResume(error -> Mono.empty())
                .block();
        if (route != null) {
            Optional<RouteDefinition> routeDefinition = routeDefinitionMapper.toRouteDefinition(route);
            if (routeDefinition.isPresent()) {
                routeDefinitionRepository.save(Mono.just(routeDefinition.get())).block();
                log.info("Gateway route refreshed: routeId={}", routeId);
            }
        }
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    public void deleteRoute(UUID routeId) {
        routeDefinitionRepository.delete(Mono.just(routeId.toString())).block();
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Gateway route deleted: routeId={}", routeId);
    }
}
