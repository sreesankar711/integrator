package com.integrator.gateway;

import com.integrator.gateway.client.RouteServiceClient;
import com.integrator.gateway.config.GatewayProperties;
import com.integrator.gateway.dto.Route;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RouteServiceClientTest {

    @Test
    @Order(1)
    @DisplayName("All routes are returned from Route Service")
    void getAllRoutesSuccess() {
        UUID routeId = UUID.randomUUID();
        RouteServiceClient client = clientWithResponse("""
                {
                  "success": true,
                  "data": [
                    {
                      "id": "%s",
                      "name": "orders",
                      "pathPattern": "/orders/**",
                      "httpMethod": "GET",
                      "targetUrl": "http://localhost:9000",
                      "transformType": "NONE",
                      "enabled": true
                    }
                  ]
                }
                """.formatted(routeId));
        List<Route> routes = client.getAllRoutes();
        assertThat(routes).hasSize(1);
        assertThat(routes.getFirst().getId()).isEqualTo(routeId);
        assertThat(routes.getFirst().getName()).isEqualTo("orders");
    }

    @Test
    @Order(2)
    @DisplayName("Empty list is returned when all routes response has no data")
    void getAllRoutesSuccessNoData() {
        RouteServiceClient client = clientWithResponse("""
                {
                  "success": true,
                  "data": null
                }
                """);

        List<Route> routes = client.getAllRoutes();
        assertThat(routes).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Route is returned from Route Service")
    void getRouteSuccess() {
        UUID routeId = UUID.randomUUID();
        RouteServiceClient client = clientWithResponse("""
                {
                  "success": true,
                  "data": {
                    "id": "%s",
                    "name": "orders",
                    "pathPattern": "/orders/**",
                    "httpMethod": "GET",
                    "targetUrl": "http://localhost:9000",
                    "transformType": "NONE",
                    "enabled": true
                  }
                }
                """.formatted(routeId));
        Route route = client.getRoute(routeId);
        assertThat(route).isNotNull();
        assertThat(route.getId()).isEqualTo(routeId);
        assertThat(route.getName()).isEqualTo("orders");
    }

    @Test
    @Order(4)
    @DisplayName("Null is returned when route response has no data")
    void getRouteSuccessNoData() {
        RouteServiceClient client = clientWithResponse("""
                {
                  "success": true,
                  "data": null
                }
                """);
        Route route = client.getRoute(UUID.randomUUID());
        assertThat(route).isNull();
    }

    private RouteServiceClient clientWithResponse(String json) {
        GatewayProperties gatewayProperties = new GatewayProperties();
        gatewayProperties.setRouteServiceUrl("http://route-service");
        WebClient webClient = WebClient.builder()
                .baseUrl(gatewayProperties.getRouteServiceUrl())
                .exchangeFunction(request -> Mono.just(jsonResponse(json)))
                .build();
        return new RouteServiceClient(gatewayProperties, webClient);
    }

    private ClientResponse jsonResponse(String json) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }
}