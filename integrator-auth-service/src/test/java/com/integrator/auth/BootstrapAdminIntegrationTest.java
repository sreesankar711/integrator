package com.integrator.auth;

import com.integrator.auth.dto.LoginRequest;
import com.integrator.auth.dto.RegisterRequest;
import com.integrator.auth.dto.TokenPair;
import com.integrator.auth.dto.UserDto;
import com.integrator.auth.model.Role;
import com.integrator.auth.model.User;
import com.integrator.common.api.ApiResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapAdminIntegrationTest extends AbstractContainerBaseTest{
     private static String adminAccessToken;

    @Test
    @Order(1)
    @DisplayName("Create ADMIN user from bootstrap configuration")
    void bootstrapAdminCreated() {
        Optional<User> admin = userRepository.findByUsername(ADMIN_USERNAME);
        assertThat(admin).isPresent();
        assertThat(admin.get().getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(admin.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.get().getPassword())).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Allow seeded ADMIN user to login with ADMIN role in token")
    void bootstrapAdminCanLoginWithAdminToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(ADMIN_USERNAME);
        loginRequest.setPassword(ADMIN_PASSWORD);
        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        Claims claims = jwtService.validateAccessToken(response.getBody().getData().getAccessToken());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.ADMIN.name());
        assertThat(response.getBody().getData().getAccessToken()).isNotNull();
        adminAccessToken = response.getBody().getData().getAccessToken();
    }

    @Test
    @Order(3)
    @DisplayName("Admin user can view actuator paths")
    void adminUserCanViewActuatorPaths() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        ResponseEntity<Object> response = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("other users cant view actuator paths")
    void otherUserCantViewActuatorPaths() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("accTester");
        registerRequest.setPassword("password");
        registerRequest.setEmail("Acc@integrator.dev");
        ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("accTester");
        loginRequest.setPassword("password");
        ResponseEntity<ApiResponse<TokenPair>> loginResponse = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getData().getAccessToken()).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().getData().getAccessToken());
        ResponseEntity<Object> actuatorTestResponse = restTemplate.exchange(
                "http://localhost:" + port + "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(actuatorTestResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
