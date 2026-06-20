package com.integrator.route;

import com.integrator.common.api.ApiResponse;
import com.integrator.common.api.PagedResponse;
import com.integrator.common.event.RouteEvent;
import com.integrator.common.event.RouteEventType;
import com.integrator.route.dto.CreateRouteRequest;
import com.integrator.route.dto.RouteResponse;
import com.integrator.route.dto.UpdateRouteRequest;
import com.integrator.route.model.Route;
import com.integrator.route.model.TransformType;
import com.integrator.route.repository.RouteRepository;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RouteIntegrationTest extends AbstractContainerBaseTest {
    @Autowired
    private RouteRepository routeRepository;

    @Test
    @Order(1)
    @DisplayName("Return Success when route is created successfully")
    void createRouteSuccess() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("route1");
        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getRoutingRules()).isEmpty();
        compareRoute(response.getBody().getData(), createRouteRequest);

        Optional<Route> savedRoute = routeRepository.findByName(createRouteRequest.getName());
        assertThat(savedRoute).isPresent();
        assertThat(response.getBody().getData().getId()).isEqualTo(savedRoute.get().getId());
        compareRoute(savedRoute.get(), createRouteRequest);

        ConsumerRecord<String, RouteEvent> event = consumeRouteEvent(savedRoute.get().getId(), RouteEventType.CREATED);
        assertThat(event).isNotNull();
        assertThat(event.key()).isEqualTo(savedRoute.get().getId().toString());
        assertThat(event.value()).isNotNull();
        assertThat(event.value().getRouteId()).isEqualTo(savedRoute.get().getId());
        assertThat(event.value().getRouteEventType()).isEqualTo(RouteEventType.CREATED);
        assertThat(event.value().getEventId()).isEqualTo(response.getBody().getCorrelationId());
    }

    @Test
    @Order(2)
    @DisplayName("Return Failure when routeName is duplicate")
    void createRouteFailureDuplicateName() {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();
        createRouteRequest.setName("route1");
        createRouteRequest.setDescription("route1");
        createRouteRequest.setPathPattern("/path/**");
        createRouteRequest.setHttpMethod(RequestMethod.GET);
        createRouteRequest.setTargetUrl("http://route-service");
        createRouteRequest.setTransformType(TransformType.NONE);
        createRouteRequest.setEnabled(true);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Route name already exists");
    }

    @Test
    @Order(3)
    @DisplayName("Return Failure when requestBody validation fails")
    void createRouteFailureRequestBodyValidationFails() {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();

        ResponseEntity<ApiResponse<Map<String, String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).containsEntry("name", "must not be blank");
        assertThat(response.getBody().getData()).containsEntry("pathPattern", "must not be blank");
        assertThat(response.getBody().getData()).containsEntry("transformType", "must not be null");
        assertThat(response.getBody().getData()).containsEntry("httpMethod", "must not be null");
        assertThat(response.getBody().getData()).containsEntry("targetUrl", "must not be blank");
        assertThat(response.getBody().getData()).containsEntry("enabled", "must not be null");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @Order(4)
    @DisplayName("Return Failure when auth token is missing")
    void createRouteFailureAuthTokenMissing() {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();

        ResponseEntity<ApiResponse<Map<String, String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(5)
    @DisplayName("Return Failure when auth token is invalid")
    void createRouteFailureAuthTokenInvalid() {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid");

        ResponseEntity<ApiResponse<Map<String, String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, headers),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(6)
    @DisplayName("Return Success when all Routes are fetched")
    void getRoutesSuccess() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("route2");
        restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        ResponseEntity<ApiResponse<List<RouteResponse>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).extracting(RouteResponse::getName).contains("route1", "route2");

        Optional<Route> savedRoute = routeRepository.findByName("route1");
        assertThat(savedRoute).isPresent();
        Optional<RouteResponse> routeResponse = response.getBody().getData()
                .stream()
                .filter(route -> route.getName().equals("route1"))
                .findFirst();
        assertThat(routeResponse).isPresent();
        compareRoute(routeResponse.get(), savedRoute.get());
    }

    @Test
    @Order(7)
    @DisplayName("Return Success when all Routes are fetched in paged")
    void getPagedRoutesSuccess() {
        int page = 0;
        int size = 1;
        ResponseEntity<PagedResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes?page=" + page + "&size=" + size,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();

        int numRoutes = routeRepository.findAll().size();
        assertThat(response.getBody().getPageNumber()).isEqualTo(page);
        assertThat(response.getBody().getPageSize()).isEqualTo(size);
        assertThat(response.getBody().getTotalElements()).isEqualTo(numRoutes);
        assertThat(response.getBody().isLast()).isFalse();
        assertThat(response.getBody().getData()).isNotNull();

        String routeName = response.getBody().getData().getFirst().getName();
        Optional<Route> savedRoute = routeRepository.findByName(routeName);
        assertThat(savedRoute).isPresent();
        compareRoute(response.getBody().getData().getFirst(), savedRoute.get());
    }

    @Test
    @Order(8)
    @DisplayName("Return Success when Route is fetched by id successfully")
    void getRouteByIdSuccess() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("route3");
        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + response.getBody().getData().getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isTrue();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isEqualTo(response.getBody().getData());
    }

    @Test
    @Order(9)
    @DisplayName("Return Failure when routeId does not exist")
    void getRouteByIdFailureRouteIdDoesNotExist() {
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + UUID.randomUUID().toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(10)
    @DisplayName("Return Failure when routeId is invalid")
    void getRouteByIdFailureRouteIdIsInvalid() {
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/invalid-routeId",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(11)
    @DisplayName("Return Success when route is updated")
    void updateRouteSuccess() {
        Optional<Route> routeForId = routeRepository.findByName("route2");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("route5");
        updateRouteRequest.setDescription("route5");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isTrue();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNotNull();

        assertThat(routeResponse.getBody().getData().getName()).isEqualTo(updateRouteRequest.getName());
        assertThat(routeResponse.getBody().getData().getDescription()).isEqualTo(updateRouteRequest.getDescription());
        assertThat(routeResponse.getBody().getData().getPathPattern()).isEqualTo(updateRouteRequest.getPathPattern());
        assertThat(routeResponse.getBody().getData().getTargetUrl()).isEqualTo(updateRouteRequest.getTargetUrl());
        assertThat(routeResponse.getBody().getData().getHttpMethod()).isEqualTo(updateRouteRequest.getHttpMethod());
        assertThat(routeResponse.getBody().getData().isEnabled()).isEqualTo(updateRouteRequest.getEnabled());
        assertThat(routeResponse.getBody().getData().getTransformType()).isEqualTo(updateRouteRequest.getTransformType());

        Optional<Route> savedRoute = routeRepository.findByName(updateRouteRequest.getName());
        assertThat(savedRoute).isPresent();
        Route route = savedRoute.get();
        compareRoute(routeResponse.getBody().getData(), route);

        ConsumerRecord<String, RouteEvent> event = consumeRouteEvent(id, RouteEventType.UPDATED);
        assertThat(event).isNotNull();
        assertThat(event.key()).isEqualTo(id.toString());
        assertThat(event.value()).isNotNull();
        assertThat(event.value().getRouteId()).isEqualTo(id);
        assertThat(event.value().getRouteEventType()).isEqualTo(RouteEventType.UPDATED);
        assertThat(event.value().getEventId()).isEqualTo(routeResponse.getBody().getCorrelationId());
    }

    @Test
    @Order(12)
    @DisplayName("Return Failure when routeName already exists")
    void updateRouteFailureRouteNameAlreadyExists() {
        Optional<Route> routeForId = routeRepository.findByName("route5");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        assertThat(routeRepository.findByName("route1")).isPresent();
        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("route1");
        updateRouteRequest.setDescription("duplicate route name");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route name already exists");
    }

    @Test
    @Order(13)
    @DisplayName("Return Failure in updateRoute when routeId is invalid")
    void updateRouteFailureRouteIdIsInvalid() {
        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("route5");
        updateRouteRequest.setDescription("route5");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/invalid-routeId",
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(14)
    @DisplayName("Return Failure in updateRoute when route is not found")
    void updateRouteFailureRouteIdNotFound() {
        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("updateFail");
        updateRouteRequest.setDescription("updateFail");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(15)
    @DisplayName("Return Failure when route is updated but token missing")
    void updateRouteFailureAuthTokenMissing() {
        Optional<Route> routeForId = routeRepository.findByName("route5");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("route5");
        updateRouteRequest.setDescription("route5");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(16)
    @DisplayName("Return Failure when route is updated but token invalid")
    void updateRouteFailureAuthTokenInvalid() {
        Optional<Route> routeForId = routeRepository.findByName("route5");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("route5");
        updateRouteRequest.setDescription("route5");
        updateRouteRequest.setPathPattern("/new/path");
        updateRouteRequest.setTargetUrl("https://example.com/new/url");
        updateRouteRequest.setHttpMethod(RequestMethod.DELETE);
        updateRouteRequest.setEnabled(Boolean.FALSE);
        updateRouteRequest.setTransformType(TransformType.NONE);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid");

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(17)
    @DisplayName("Return Success when route is deleted")
    void deleteRouteSuccess() {
        Optional<Route> routeForId = routeRepository.findByName("route5");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isTrue();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();

        Optional<Route> savedRoute = routeRepository.findById(id);
        assertThat(savedRoute).isNotPresent();

        ConsumerRecord<String, RouteEvent> event = consumeRouteEvent(id, RouteEventType.DELETED);
        assertThat(event).isNotNull();
        assertThat(event.key()).isEqualTo(id.toString());
        assertThat(event.value()).isNotNull();
        assertThat(event.value().getRouteId()).isEqualTo(id);
        assertThat(event.value().getRouteEventType()).isEqualTo(RouteEventType.DELETED);
        assertThat(event.value().getEventId()).isEqualTo(routeResponse.getBody().getCorrelationId());
    }

    @Test
    @Order(18)
    @DisplayName("Return Failure in deleteRoute when routeId is invalid")
    void deleteRouteFailureRouteIdIsInvalid() {
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/invalid-routeId",
                HttpMethod.DELETE,
                new HttpEntity<>(userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(19)
    @DisplayName("Return Failure in deleteRoute when routeId not found")
    void deleteRouteFailureRouteIdNotFound() {
        UUID id = UUID.randomUUID();
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(routeResponse.getBody()).isNotNull();
        assertThat(routeResponse.getBody().isSuccess()).isFalse();
        assertThat(routeResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(routeResponse.getBody().getTimestamp()).isNotNull();
        assertThat(routeResponse.getBody().getData()).isNull();
        assertThat(routeResponse.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(20)
    @DisplayName("Return Failure in deleteRoute when auth token is missing")
    void deleteRouteFailureAuthTokenMissing() {
        UUID id = UUID.randomUUID();
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(21)
    @DisplayName("Return Failure in deleteRoute when auth token is invalid")
    void deleteRouteFailureAuthTokenInvalid() {
        UUID id = UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid");
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(routeResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(22)
    @DisplayName("Return Success when route is created with rate limits")
    void createRouteSuccessWithRateLimits() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("rateLimitRoute1");
        createRouteRequest.setRateLimitEnabled(true);
        createRouteRequest.setRateLimitReplenishRate(10);
        createRouteRequest.setRateLimitBurstCapacity(20);
        createRouteRequest.setRateLimitRequestedTokens(2);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        compareRoute(response.getBody().getData(), createRouteRequest);

        Optional<Route> savedRoute = routeRepository.findByName(createRouteRequest.getName());
        assertThat(savedRoute).isPresent();
        compareRoute(savedRoute.get(), createRouteRequest);
    }

    @Test
    @Order(23)
    @DisplayName("Return Failure when rate limit values are missing")
    void createRouteFailureRateLimitValuesMissing() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("rateLimitRouteMissing");
        createRouteRequest.setRateLimitEnabled(true);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Rate limit values are required when rate limiting is enabled");
    }

    @Test
    @Order(24)
    @DisplayName("Return Failure when rate limit values are not positive")
    void createRouteFailureRateLimitValuesNotPositive() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("rateLimitRouteNotPositive");
        createRouteRequest.setRateLimitEnabled(true);
        createRouteRequest.setRateLimitReplenishRate(0);
        createRouteRequest.setRateLimitBurstCapacity(20);
        createRouteRequest.setRateLimitRequestedTokens(1);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Rate limit values must be positive");
    }

    @Test
    @Order(25)
    @DisplayName("Return Failure when requested tokens exceed burst capacity")
    void createRouteFailureRequestedTokensExceedBurstCapacity() {
        CreateRouteRequest createRouteRequest = createRouteRequestBody("rateLimitRouteTokenOverflow");
        createRouteRequest.setRateLimitEnabled(true);
        createRouteRequest.setRateLimitReplenishRate(10);
        createRouteRequest.setRateLimitBurstCapacity(2);
        createRouteRequest.setRateLimitRequestedTokens(3);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Requested tokens must be less than or equal to burst capacity");
    }

    @Test
    @Order(26)
    @DisplayName("Return Success when route rate limits are updated")
    void updateRouteSuccessWithRateLimits() {
        Optional<Route> routeForId = routeRepository.findByName("rateLimitRoute1");
        assertThat(routeForId).isPresent();
        UUID id = routeForId.get().getId();

        UpdateRouteRequest updateRouteRequest = new UpdateRouteRequest();
        updateRouteRequest.setName("rateLimitRoute1Updated");
        updateRouteRequest.setDescription("rate limit updated");
        updateRouteRequest.setPathPattern("/rate-limit/updated");
        updateRouteRequest.setTargetUrl("https://example.com/rate-limit-updated");
        updateRouteRequest.setHttpMethod(RequestMethod.POST);
        updateRouteRequest.setEnabled(true);
        updateRouteRequest.setTransformType(TransformType.NONE);
        updateRouteRequest.setRateLimitEnabled(true);
        updateRouteRequest.setRateLimitReplenishRate(30);
        updateRouteRequest.setRateLimitBurstCapacity(60);
        updateRouteRequest.setRateLimitRequestedTokens(3);

        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateRouteRequest, userHeader()),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isRateLimitEnabled()).isTrue();
        assertThat(response.getBody().getData().getRateLimitReplenishRate()).isEqualTo(30);
        assertThat(response.getBody().getData().getRateLimitBurstCapacity()).isEqualTo(60);
        assertThat(response.getBody().getData().getRateLimitRequestedTokens()).isEqualTo(3);

        Optional<Route> savedRoute = routeRepository.findByName(updateRouteRequest.getName());
        assertThat(savedRoute).isPresent();
        compareRoute(response.getBody().getData(), savedRoute.get());
    }

    private void compareRoute(Route route, CreateRouteRequest createRouteRequest) {
        assertThat(route.getName()).isEqualTo(createRouteRequest.getName());
        assertThat(route.getDescription()).isEqualTo(createRouteRequest.getDescription());
        assertThat(route.getPathPattern()).isEqualTo(createRouteRequest.getPathPattern());
        assertThat(route.getHttpMethod()).isEqualTo(createRouteRequest.getHttpMethod());
        assertThat(route.getTargetUrl()).isEqualTo(createRouteRequest.getTargetUrl());
        assertThat(route.getTransformType()).isEqualTo(createRouteRequest.getTransformType());
        assertThat(route.isEnabled()).isEqualTo(createRouteRequest.getEnabled());
        assertThat(route.isRateLimitEnabled()).isEqualTo(Boolean.TRUE.equals(createRouteRequest.getRateLimitEnabled()));
        assertThat(route.getRateLimitReplenishRate()).isEqualTo(createRouteRequest.getRateLimitReplenishRate());
        assertThat(route.getRateLimitBurstCapacity()).isEqualTo(createRouteRequest.getRateLimitBurstCapacity());
        assertThat(route.getRateLimitRequestedTokens()).isEqualTo(createRouteRequest.getRateLimitRequestedTokens());
    }

    private void compareRoute(RouteResponse routeResponse, CreateRouteRequest createRouteRequest) {
        assertThat(routeResponse.getName()).isEqualTo(createRouteRequest.getName());
        assertThat(routeResponse.getDescription()).isEqualTo(createRouteRequest.getDescription());
        assertThat(routeResponse.getPathPattern()).isEqualTo(createRouteRequest.getPathPattern());
        assertThat(routeResponse.getHttpMethod()).isEqualTo(createRouteRequest.getHttpMethod());
        assertThat(routeResponse.getTargetUrl()).isEqualTo(createRouteRequest.getTargetUrl());
        assertThat(routeResponse.getTransformType()).isEqualTo(createRouteRequest.getTransformType());
        assertThat(routeResponse.isEnabled()).isEqualTo(createRouteRequest.getEnabled());
        assertThat(routeResponse.isRateLimitEnabled()).isEqualTo(Boolean.TRUE.equals(createRouteRequest.getRateLimitEnabled()));
        assertThat(routeResponse.getRateLimitReplenishRate()).isEqualTo(createRouteRequest.getRateLimitReplenishRate());
        assertThat(routeResponse.getRateLimitBurstCapacity()).isEqualTo(createRouteRequest.getRateLimitBurstCapacity());
        assertThat(routeResponse.getRateLimitRequestedTokens()).isEqualTo(createRouteRequest.getRateLimitRequestedTokens());
    }

    private void compareRoute(RouteResponse routeResponse, Route route) {
        assertThat(routeResponse.getName()).isEqualTo(route.getName());
        assertThat(routeResponse.getDescription()).isEqualTo(route.getDescription());
        assertThat(routeResponse.getPathPattern()).isEqualTo(route.getPathPattern());
        assertThat(routeResponse.getHttpMethod()).isEqualTo(route.getHttpMethod());
        assertThat(routeResponse.getTargetUrl()).isEqualTo(route.getTargetUrl());
        assertThat(routeResponse.getTransformType()).isEqualTo(route.getTransformType());
        assertThat(routeResponse.isEnabled()).isEqualTo(route.isEnabled());
        assertThat(routeResponse.getCreatedAt()).isEqualTo(route.getCreatedAt());
        assertThat(routeResponse.getUpdatedAt()).isEqualTo(route.getUpdatedAt());
        assertThat(routeResponse.isRateLimitEnabled()).isEqualTo(route.isRateLimitEnabled());
        assertThat(routeResponse.getRateLimitReplenishRate()).isEqualTo(route.getRateLimitReplenishRate());
        assertThat(routeResponse.getRateLimitBurstCapacity()).isEqualTo(route.getRateLimitBurstCapacity());
        assertThat(routeResponse.getRateLimitRequestedTokens()).isEqualTo(route.getRateLimitRequestedTokens());
    }

    private CreateRouteRequest createRouteRequestBody(String name) {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();
        createRouteRequest.setName(name);
        createRouteRequest.setDescription("description");
        createRouteRequest.setPathPattern("/path/**");
        createRouteRequest.setHttpMethod(RequestMethod.GET);
        createRouteRequest.setTargetUrl("http://route-service");
        createRouteRequest.setTransformType(TransformType.NONE);
        createRouteRequest.setEnabled(true);

        return createRouteRequest;
    }

    private ConsumerRecord<String, RouteEvent> consumeRouteEvent(UUID routeId, RouteEventType routeEventType) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "route-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class.getName());
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.integrator.route.event");
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, RouteEvent.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        try (Consumer<String, RouteEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singleton("route.events"));
                ConsumerRecords<String, RouteEvent> consumerRecords = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, RouteEvent> consumerRecord : consumerRecords) {
                    RouteEvent event = consumerRecord.value();
                    if (event != null
                            && routeId.equals(event.getRouteId())
                            && routeEventType == event.getRouteEventType()) {
                        return consumerRecord;
                    }
            }
        }
        return null;
    }
}
