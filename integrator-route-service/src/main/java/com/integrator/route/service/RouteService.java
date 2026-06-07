package com.integrator.route.service;

import com.integrator.common.event.RouteEvent;
import com.integrator.common.event.RouteEventType;
import com.integrator.common.exception.ResourceNotFoundException;
import com.integrator.common.exception.ValidationException;
import com.integrator.route.dto.CreateRouteRequest;
import com.integrator.route.dto.RouteResponse;
import com.integrator.route.dto.RoutingRuleResponse;
import com.integrator.route.dto.UpdateRouteRequest;
import com.integrator.route.model.Route;
import com.integrator.route.model.RoutingRule;
import com.integrator.route.repository.RouteRepository;
import com.integrator.route.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {
    private final RouteRepository routeRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String ROUTE_NOT_FOUND = "Route not found";
    private static final String ROUTE_NAME_EXISTS = "Route name already exists";

    public RouteResponse createRoute(CreateRouteRequest createRouteRequest) {
        if (routeRepository.existsByName(createRouteRequest.getName())) {
            throw new ValidationException(ROUTE_NAME_EXISTS);
        }
            Route route = Route.builder()
                .name(createRouteRequest.getName())
                .description(createRouteRequest.getDescription())
                .pathPattern(createRouteRequest.getPathPattern())
                .httpMethod(createRouteRequest.getHttpMethod())
                .targetUrl(createRouteRequest.getTargetUrl())
                .transformType(createRouteRequest.getTransformType())
                .fieldMappingConfig(createRouteRequest.getFieldMappingConfig())
                .snippetId(createRouteRequest.getSnippetId())
                .enabled(createRouteRequest.getEnabled())
                .build();

        Route savedRoute = routeRepository.saveAndFlush(route);
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.CREATED, savedRoute.getId()));
        return routeResponseBuilder(savedRoute, List.of());
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(route -> routeResponseBuilder(
                        route,
                        routingRuleRepository.findByRouteIdOrderByPriorityAsc(route.getId())
                )).toList();
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> getAllRoutes(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Route> routePage = routeRepository.findAll(pageable);

        return routePage.map(route -> routeResponseBuilder(
                        route,
                        routingRuleRepository.findByRouteIdOrderByPriorityAsc(route.getId())
                )
        );
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND));
        return routeResponseBuilder(route,routingRuleRepository.findByRouteIdOrderByPriorityAsc(routeId));
    }

    public RouteResponse updateRoute(UUID routeId, UpdateRouteRequest updateRouteRequest) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND));

        Optional<Route> savedRoute = routeRepository.findByName(updateRouteRequest.getName());
        if(savedRoute.isPresent() && !savedRoute.get().getId().equals(route.getId())) {
            throw new ValidationException(ROUTE_NAME_EXISTS);
        }

        Route updatedRoute = route.toBuilder()
                .name(updateRouteRequest.getName())
                .description(updateRouteRequest.getDescription())
                .pathPattern(updateRouteRequest.getPathPattern())
                .httpMethod(updateRouteRequest.getHttpMethod())
                .targetUrl(updateRouteRequest.getTargetUrl())
                .transformType(updateRouteRequest.getTransformType())
                .fieldMappingConfig(updateRouteRequest.getFieldMappingConfig())
                .snippetId(updateRouteRequest.getSnippetId())
                .enabled(updateRouteRequest.getEnabled())
                .build();

        RouteResponse routeResponse = routeResponseBuilder(routeRepository.saveAndFlush(updatedRoute),
                routingRuleRepository.findByRouteIdOrderByPriorityAsc(updatedRoute.getId()));
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.UPDATED, routeId));
        return routeResponse;
    }

    public void deleteRoute(UUID routeId) {
        routeRepository.delete(routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_NOT_FOUND)));
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.DELETED, routeId));
    }

    private RouteResponse routeResponseBuilder(Route route, List<RoutingRule> routingRules) {
        return RouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .pathPattern(route.getPathPattern())
                .httpMethod(route.getHttpMethod())
                .targetUrl(route.getTargetUrl())
                .transformType(route.getTransformType())
                .fieldMappingConfig(route.getFieldMappingConfig())
                .snippetId(route.getSnippetId())
                .enabled(route.isEnabled())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .routingRules(routingRules.stream().map(this::routingRuleResponseBuilder).toList())
                .build();
    }

    private RoutingRuleResponse routingRuleResponseBuilder(RoutingRule rule) {
        RoutingRuleResponse response = new RoutingRuleResponse();
        response.setId(rule.getId());
        response.setRouteId(rule.getRoute().getId());
        response.setMatchConfig(rule.getMatchConfig());
        response.setOverrideTargetUrl(rule.getOverrideTargetUrl());
        response.setPriority(rule.getPriority());
        response.setEnabled(rule.isEnabled());
        response.setCreatedAt(rule.getCreatedAt());
        return response;
    }
}
