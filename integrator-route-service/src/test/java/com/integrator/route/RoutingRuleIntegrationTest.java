package com.integrator.route;

import com.integrator.common.api.ApiResponse;
import com.integrator.route.dto.*;
import com.integrator.route.model.Route;
import com.integrator.route.model.RoutingRule;
import com.integrator.route.model.TransformType;
import com.integrator.route.repository.RouteRepository;
import com.integrator.route.repository.RoutingRuleRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoutingRuleIntegrationTest extends AbstractContainerBaseTest{
    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RoutingRuleRepository routingRuleRepository;

    private UUID routeId;

    @BeforeAll
    public void beforeAll() {
        CreateRouteRequest createRouteRequest = new CreateRouteRequest();
        createRouteRequest.setName("routeTest");
        createRouteRequest.setDescription("description");
        createRouteRequest.setPathPattern("/path/**");
        createRouteRequest.setHttpMethod(RequestMethod.GET);
        createRouteRequest.setTargetUrl("http://route-service");
        createRouteRequest.setTransformType(TransformType.NONE);
        createRouteRequest.setEnabled(true);


        ResponseEntity<ApiResponse<RouteResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/routes",
                HttpMethod.POST,
                new HttpEntity<>(createRouteRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();

        routeId = response.getBody().getData().getId();
    }

    @Test
    @Order(1)
    @DisplayName("Return Success if the routingRule is created")
    public void createRoutingRuleSuccess() {
        CreateRoutingRuleRequest createRoutingRuleRequest = new CreateRoutingRuleRequest();
        createRoutingRuleRequest.setMatchConfig("{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-a\"}]}");
        createRoutingRuleRequest.setEnabled(true);
        createRoutingRuleRequest.setOverrideTargetUrl("http://route-service");
        createRoutingRuleRequest.setPriority(0);

        ResponseEntity<ApiResponse<RoutingRuleResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/create?routeId=" + routeId,
                HttpMethod.POST,
                new HttpEntity<>(createRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();

        Optional<Route> savedRoute = routeRepository.findById(routeId);
        assertThat(savedRoute).isPresent();
        Route route = savedRoute.get();

        assertThat(response.getBody().getData().getRouteId()).isEqualTo(route.getId());
        JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(response.getBody().getData().getMatchConfig(), createRoutingRuleRequest.getMatchConfig());
        assertThat(response.getBody().getData().getOverrideTargetUrl()).isEqualTo(createRoutingRuleRequest.getOverrideTargetUrl());
        assertThat(response.getBody().getData().getPriority()).isEqualTo(createRoutingRuleRequest.getPriority());
        assertThat(response.getBody().getData().isEnabled()).isEqualTo(createRoutingRuleRequest.getEnabled());

        Optional<RoutingRule> savedRoutingRule = routingRuleRepository.findById(response.getBody().getData().getId());
        assertThat(savedRoutingRule).isPresent();
        RoutingRule routingRule = savedRoutingRule.get();

        JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(routingRule.getMatchConfig(), createRoutingRuleRequest.getMatchConfig());
        assertThat(routingRule.getOverrideTargetUrl()).isEqualTo(createRoutingRuleRequest.getOverrideTargetUrl());
        assertThat(routingRule.getPriority()).isEqualTo(createRoutingRuleRequest.getPriority());
        assertThat(routingRule.isEnabled()).isEqualTo(createRoutingRuleRequest.getEnabled());
        assertThat(routingRule.getRoute().getId()).isEqualTo(route.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Return Failure if the Body fails validation")
    public void createRoutingRuleFailureBodyFailsValidation() {
        Optional<Route> savedRoute = routeRepository.findById(routeId);
        assertThat(savedRoute).isPresent();
        Route route = savedRoute.get();

        CreateRoutingRuleRequest createRoutingRuleRequest = new CreateRoutingRuleRequest();

        ResponseEntity<ApiResponse<Map<String,String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/create?routeId=" + route.getId(),
                HttpMethod.POST,
                new HttpEntity<>(createRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().get("matchConfig")).isEqualTo("must not be blank");
        assertThat(response.getBody().getData().get("overrideTargetUrl")).isEqualTo("must not be blank");
        assertThat(response.getBody().getData().get("priority")).isEqualTo("must not be null");
        assertThat(response.getBody().getData().get("enabled")).isEqualTo("must not be null");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @Order(3)
    @DisplayName("Return Failure if the Route not found")
    public void createRoutingRuleFailureRouteNotFound() {
        CreateRoutingRuleRequest createRoutingRuleRequest = new CreateRoutingRuleRequest();
        createRoutingRuleRequest.setMatchConfig("{\"matchMode\": \"ALL\",\"conditions\": [{\"type\": \"HEADER\",\"key\": \"X-Client\",\"equals\": \"partner-a\"}]}");
        createRoutingRuleRequest.setEnabled(true);
        createRoutingRuleRequest.setOverrideTargetUrl("http://route-service");
        createRoutingRuleRequest.setPriority(0);

        ResponseEntity<ApiResponse<Map<String,String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/create?routeId=invalid-routeId",
                HttpMethod.POST,
                new HttpEntity<>(createRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(4)
    @DisplayName("Return Failure if the routeId not given")
    public void createRoutingRuleFailureRouteIdNotGiven() {
        CreateRoutingRuleRequest createRoutingRuleRequest = new CreateRoutingRuleRequest();
        createRoutingRuleRequest.setMatchConfig("{\"matchMode\":\"ALL\",\"conditions\":[{\"type\":\"HEADER\",\"key\":\"X-Client\",\"equals\":\"partner-a\"}]}");
        createRoutingRuleRequest.setEnabled(true);
        createRoutingRuleRequest.setOverrideTargetUrl("http://route-service");
        createRoutingRuleRequest.setPriority(0);

        ResponseEntity<ApiResponse<Map<String,String>>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/create",
                HttpMethod.POST,
                new HttpEntity<>(createRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Route not found");
    }

    @Test
    @Order(5)
    @DisplayName("Return Success if routingRule is updated")
    public void updateRoutingRuleSuccess() {
        Optional<Route> savedRoute = routeRepository.findById(routeId);
        assertThat(savedRoute).isPresent();
        Route route = savedRoute.get();
        List<RoutingRule> routingRule = routingRuleRepository.findByRouteIdOrderByPriorityAsc(route.getId());
        RoutingRule savedRoutingRule = routingRule.getFirst();

        UpdateRoutingRuleRequest updateRoutingRuleRequest = new UpdateRoutingRuleRequest();
        updateRoutingRuleRequest.setMatchConfig("{\"matchMode\": \"NONE\"}");
        updateRoutingRuleRequest.setEnabled(false);
        updateRoutingRuleRequest.setOverrideTargetUrl("http://route-service-updated");
        updateRoutingRuleRequest.setPriority(1);

        ResponseEntity<ApiResponse<RoutingRuleResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/" +  savedRoutingRule.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(response.getBody().getData().getMatchConfig(), updateRoutingRuleRequest.getMatchConfig());
        assertThat(response.getBody().getData().getPriority()).isEqualTo(updateRoutingRuleRequest.getPriority());
        assertThat(response.getBody().getData().isEnabled()).isEqualTo(updateRoutingRuleRequest.getEnabled());
        assertThat(response.getBody().getData().getOverrideTargetUrl()).isEqualTo(updateRoutingRuleRequest.getOverrideTargetUrl());

        Optional<RoutingRule>  updatedRoutingRule = routingRuleRepository.findById(savedRoutingRule.getId());
        assertThat(updatedRoutingRule).isPresent();
        JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(updatedRoutingRule.get().getMatchConfig(), updateRoutingRuleRequest.getMatchConfig());
        assertThat(updatedRoutingRule.get().getPriority()).isEqualTo(updateRoutingRuleRequest.getPriority());
        assertThat(updatedRoutingRule.get().isEnabled()).isEqualTo(updateRoutingRuleRequest.getEnabled());
        assertThat(updatedRoutingRule.get().getOverrideTargetUrl()).isEqualTo(updateRoutingRuleRequest.getOverrideTargetUrl());
    }

    @Test
    @Order(6)
    @DisplayName("Return Failure if rule is not found")
    public void updateRoutingRuleFailureRuleNotFound() {
        UpdateRoutingRuleRequest updateRoutingRuleRequest = new UpdateRoutingRuleRequest();
        updateRoutingRuleRequest.setMatchConfig("{\"matchMode\":\"NONE\"}");
        updateRoutingRuleRequest.setEnabled(false);
        updateRoutingRuleRequest.setOverrideTargetUrl("http://route-service-updated");
        updateRoutingRuleRequest.setPriority(1);

        ResponseEntity<ApiResponse<RoutingRuleResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/invalid-ruleId",
                HttpMethod.PUT,
                new HttpEntity<>(updateRoutingRuleRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Routing rule not found");
    }

    @Test
    @Order(7)
    @DisplayName("Return Success if rule is deleted")
    public void deleteRoutingRuleSuccess() {
        Optional<Route> savedRoute = routeRepository.findById(routeId);
        assertThat(savedRoute).isPresent();
        Route route = savedRoute.get();
        List<RoutingRule> routingRule = routingRuleRepository.findByRouteIdOrderByPriorityAsc(route.getId());
        RoutingRule savedRoutingRule = routingRule.getFirst();

        ResponseEntity<ApiResponse<RoutingRuleResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/" +   savedRoutingRule.getId(),
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(routingRuleRepository.findById(savedRoutingRule.getId())).isNotPresent();
    }

    @Test
    @Order(8)
    @DisplayName("Return Failure if rule not found")
    public void deleteRoutingRuleFailureRuleNotFound() {
        ResponseEntity<ApiResponse<RoutingRuleResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/rules/invalid-ruleId",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Routing rule not found");
    }
}
