package com.integrator.route.service;

import com.integrator.common.event.RouteEvent;
import com.integrator.common.event.RouteEventType;
import com.integrator.common.exception.ResourceNotFoundException;
import com.integrator.route.dto.CreateRoutingRuleRequest;
import com.integrator.route.dto.RoutingRuleResponse;
import com.integrator.route.dto.UpdateRoutingRuleRequest;
import com.integrator.route.model.Route;
import com.integrator.route.model.RoutingRule;
import com.integrator.route.repository.RouteRepository;
import com.integrator.route.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutingRuleService {
    private final RouteRepository routeRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RoutingRuleResponse createRoutingRule(UUID routeId, CreateRoutingRuleRequest createRoutingRuleRequest) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        RoutingRule rule = RoutingRule.builder()
                .route(route)
                .matchConfig(createRoutingRuleRequest.getMatchConfig())
                .overrideTargetUrl(createRoutingRuleRequest.getOverrideTargetUrl())
                .priority(createRoutingRuleRequest.getPriority())
                .enabled(createRoutingRuleRequest.getEnabled())
                .build();
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.UPDATED, routeId));
        return routingRuleResponseBuilder(routingRuleRepository.saveAndFlush(rule));
    }

    public RoutingRuleResponse updateRoutingRule(UUID ruleId, UpdateRoutingRuleRequest updateRoutingRuleRequest) {
        RoutingRule rule = routingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found"));

        rule.setMatchConfig(updateRoutingRuleRequest.getMatchConfig());
        rule.setOverrideTargetUrl(updateRoutingRuleRequest.getOverrideTargetUrl());
        rule.setPriority(updateRoutingRuleRequest.getPriority());
        rule.setEnabled(updateRoutingRuleRequest.getEnabled());
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.UPDATED, rule.getRoute().getId()));
        return routingRuleResponseBuilder(routingRuleRepository.save(rule));
    }

    public void deleteRoutingRule(UUID ruleId) {
        RoutingRule rule = routingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found"));
        applicationEventPublisher.publishEvent(RouteEvent.of(RouteEventType.UPDATED, rule.getRoute().getId()));
        routingRuleRepository.delete(rule);
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
