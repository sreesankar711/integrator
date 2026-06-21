package com.integrator.gateway.mcp;

import com.integrator.gateway.dto.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GatewayMcpRouteTools {
    private final GatewayRouteInspectionService gatewayRouteInspectionService;

    @McpTool(
            name = "list_all_routes",
            description = "List routes from Route Service."
    )
    public List<Route> listAllRoutes(@McpToolParam(required = false, description = "Only include enabled routes.") Boolean enabledOnly) {
        return gatewayRouteInspectionService.listAllRoutes(Boolean.TRUE.equals(enabledOnly));
    }

    @McpTool(
            name = "get_route",
            description = "Get a route from Route Service by route id."
    )
    public Route getRoute(@McpToolParam(required = true, description = "Route UUID.") String routeId) {
        return gatewayRouteInspectionService.getRoute(UUID.fromString(routeId));
    }

    @McpTool(
            name = "list_runtime_routes",
            description = "List runtime route definitions registered inside Gateway."
    )
    public List<RouteDefinition> listRuntimeRoutes() {
        return gatewayRouteInspectionService.listRuntimeRoutes();
    }

    @McpTool(
            name = "get_runtime_route",
            description = "Get a runtime Gateway route definition by route id."
    )
    public RouteDefinition getRuntimeRoute(@McpToolParam(required = true, description = "Route UUID.") String routeId) {
        return gatewayRouteInspectionService.getRuntimeRoute(routeId);
    }
}
