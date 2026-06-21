package com.integrator.gateway.mcp;

import com.integrator.gateway.dto.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GatewayMcpRouteResources {

    private final GatewayRouteInspectionService gatewayRouteInspectionService;
    private final ObjectMapper objectMapper;

    @McpResource(
            name = "routes",
            title = "All Routes",
            uri = "integrator://routes",
            description = "Route Service routes.",
            mimeType = "application/json"
    )
    public String routes() {
        return toJson(gatewayRouteInspectionService.listAllRoutes(false));
    }

    @McpResource(
            name = "runtime-routes",
            title = "Runtime Routes",
            uri = "integrator://runtime-routes",
            description = "Gateway runtime route definitions.",
            mimeType = "application/json"
    )
    public String runtimeRoutes() {
        return toJson(gatewayRouteInspectionService.listRuntimeRoutes());
    }

    @McpResource(
            name = "route-by-id",
            title = "Route By Id",
            uri = "integrator://routes/{routeId}",
            description = "Single Route Service route by route id.",
            mimeType = "application/json"
    )
    public String routeById(@McpArg(name = "routeId") String routeId) {
        Route route = gatewayRouteInspectionService.getRoute(UUID.fromString(routeId));
        return toJson(route);
    }

    @McpResource(
            name = "runtime-route-by-id",
            title = "Runtime Route By Id",
            uri = "integrator://runtime-routes/{routeId}",
            description = "Single Gateway runtime route definition by route id.",
            mimeType = "application/json"
    )
    public String runtimeRouteById(@McpArg(name = "routeId") String routeId) {
        RouteDefinition routeDefinition = gatewayRouteInspectionService.getRuntimeRoute(routeId);
        return toJson(routeDefinition);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize MCP resource", ex);
        }
    }
}
