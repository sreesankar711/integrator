package com.integrator.gateway.client;

import com.integrator.common.api.ApiResponse;
import com.integrator.gateway.config.GatewayProperties;
import com.integrator.gateway.dto.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouteServiceClient {
    private final GatewayProperties gatewayProperties;
    private final WebClient routeServiceWebClient;
    private static final String ROUTE_NOT_FOUND = "Route not found";

    public List<Route> getAllRoutes() {
        log.info("Fetching gateway routes from Route Service: {}", gatewayProperties.getRouteServiceUrl());
        ApiResponse<List<Route>> response = routeServiceWebClient.get()
                .uri("/routes")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Route>>>() {})
                .block();
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    public Route getRoute(UUID routeId) {
        ApiResponse<Route> response = routeServiceWebClient.get()
                .uri("/routes/" + routeId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Route>>() {})
                .block();
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData();
    }
}
