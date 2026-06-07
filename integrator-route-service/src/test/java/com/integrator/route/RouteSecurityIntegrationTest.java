package com.integrator.route;

import com.integrator.common.api.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RouteSecurityIntegrationTest extends AbstractContainerBaseTest {

    @Test
    @Order(1)
    @DisplayName("Return Success when actuator is called with ADMIN token")
    void actuatorSuccessAdminRole() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "http://localhost:" + port + "/actuator",
                HttpMethod.GET,
                new HttpEntity<>(adminHeader()),
                Object.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("Return Forbidden when actuator is called with USER token")
    void actuatorFailureUserRole() {
        ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/actuator",
                HttpMethod.GET,
                new HttpEntity<>(userHeader()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(3)
    @DisplayName("Return Unauthorized when actuator is called without token")
    void actuatorFailureAuthTokenMissing() {
        ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/actuator",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
