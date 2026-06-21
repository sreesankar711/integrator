package com.integrator.gateway.mcp;

import com.integrator.gateway.client.RouteServiceClient;
import com.integrator.gateway.dto.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GatewayRouteInspectionService {
    private final RouteServiceClient routeServiceClient;
    private final RouteDefinitionRepository routeDefinitionRepository;

    public List<Route> listAllRoutes(boolean enabledOnly) {
        return routeServiceClient.getAllRoutes().stream()
                .filter(route -> !enabledOnly || route.isEnabled())
                .toList();
    }

    public Route getRoute(UUID routeId) {
        return routeServiceClient.getRoute(routeId);
    }

    public List<RouteDefinition> listRuntimeRoutes() {
        return routeDefinitionRepository.getRouteDefinitions()
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public RouteDefinition getRuntimeRoute(String routeId) {
        return listRuntimeRoutes().stream()
                .filter(routeDefinition -> routeDefinition.getId().equals(routeId))
                .findFirst()
                .orElse(null);
    }
}
