package com.integrator.auth;

import com.integrator.auth.dto.*;
import com.integrator.auth.model.RefreshToken;
import com.integrator.auth.model.User;
import com.integrator.auth.repository.RefreshTokenRepository;
import com.integrator.auth.repository.UserRepository;
import com.integrator.auth.service.JwtService;
import com.integrator.auth.util.TokenHashUtils;
import com.integrator.common.api.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractContainerBaseTest{

    @Test
    @Order(1)
    @DisplayName("Return Success when a new user is registered")
    void registerSuccess() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("tester");
        registerRequest.setPassword("password");
        registerRequest.setEmail("user@integrator.dev");

        ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo(registerRequest.getUsername());
        assertThat(response.getBody().getData().getEmail()).isEqualTo(registerRequest.getEmail());

        Optional<User> user = userRepository.findByUsername("tester");
        assertThat(user).isPresent();

        User savedUser = user.get();
        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(savedUser.getPassword()).isNotEqualTo(registerRequest.getPassword());
        assertThat(savedUser.getPassword()).startsWith("$argon2");
    }

    @Test
    @Order(2)
    @DisplayName("Return Validation error when username already exists for a new user registration")
    void registerFailureSameUsername() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("tester");
        registerRequest.setPassword("password");
        registerRequest.setEmail("user2@integrator.dev");

        ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Username is already in use");
    }

    @Test
    @Order(3)
    @DisplayName("Return Validation error when email already exists for a new user registration")
    void registerFailureSameEmail() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("tester2");
        registerRequest.setPassword("password");
        registerRequest.setEmail("user@integrator.dev");

        ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email is already in use");
    }

    @Test
    @Order(4)
    @DisplayName("Return Bad Request when body is missing for a new user registration")
    void registerFailureValueMissing() {
        RegisterRequest registerRequest = new RegisterRequest();

        ResponseEntity<ApiResponse<User>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("must not be blank");
        assertThat(response.getBody().getData().getPassword()).isEqualTo("must not be blank");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("must not be blank");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @Order(5)
    @DisplayName("Return Bad Request when fields does not pass validation for a new user registration")
    void registerFailureFieldsFailValidation() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("test");
        registerRequest.setPassword("pwd");
        registerRequest.setEmail("user");

        ResponseEntity<ApiResponse<User>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("size must be between 5 and 50");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("must be a well-formed email address");
        assertThat(response.getBody().getData().getPassword()).isEqualTo("size must be between 8 and 255");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @Order(6)
    @DisplayName("Return Success when user is logged in")
    void loginSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("password");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getData().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getData().getTokenType()).isEqualTo("Bearer");

        String refreshTokenHashed = TokenHashUtils.hashedToken(response.getBody().getData().getRefreshToken());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHashed);
        assertThat(refreshToken).isPresent();
        RefreshToken savedRefreshToken = refreshToken.get();

        Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());
        assertThat(user).isPresent();
        User savedUser = user.get();

        assertThat(savedRefreshToken.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedRefreshToken.isRevoked()).isFalse();

        UUID userid = jwtService.extractUserId(response.getBody().getData().getAccessToken());
        assertThat(userid).isEqualTo(savedUser.getId());
    }

    @Test
    @Order(7)
    @DisplayName("Return Failure when username and password does not match")
    void loginFailure() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("wrongPassword");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    @Order(8)
    @DisplayName("Return Success when refresh is done successfully")
    void refreshSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("password");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        TokenPair tokenPair = response.getBody().getData();

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(tokenPair.getRefreshToken());

        ResponseEntity<ApiResponse<TokenPair>> refreshResponse = restTemplate.exchange(
                "http://localhost:" + port + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(refreshResponse.getBody().getTimestamp()).isNotNull();
        assertThat(refreshResponse.getBody().isSuccess()).isTrue();
        assertThat(refreshResponse.getBody().getData()).isNotNull();
        assertThat(refreshResponse.getBody().getData().getAccessToken()).isNotNull();
        assertThat(refreshResponse.getBody().getData().getRefreshToken()).isNotNull();
        assertThat(refreshResponse.getBody().getData().getTokenType()).isEqualTo("Bearer");

        String oldRefreshTokenHashed = TokenHashUtils.hashedToken(refreshRequest.getRefreshToken());
        Optional<RefreshToken> oldRefreshToken = refreshTokenRepository.findByTokenHash(oldRefreshTokenHashed);
        assertThat(oldRefreshToken).isPresent();
        RefreshToken oldSavedRefreshToken = oldRefreshToken.get();
        assertThat(oldSavedRefreshToken.isRevoked()).isTrue();

        String refreshTokenHashed = TokenHashUtils.hashedToken(refreshResponse.getBody().getData().getRefreshToken());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHashed);
        assertThat(refreshToken).isPresent();
        RefreshToken savedRefreshToken = refreshToken.get();
        assertThat(savedRefreshToken.isRevoked()).isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("Return Failure as refreshToken is already revoked")
    void refreshFailureRevokedToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("password");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        TokenPair tokenPair = response.getBody().getData();

        String refreshTokenHashed = TokenHashUtils.hashedToken(tokenPair.getRefreshToken());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHashed);
        assertThat(refreshToken).isPresent();
        RefreshToken refreshTokenSaved = refreshToken.get();
        refreshTokenSaved.setRevoked(true);
        refreshTokenRepository.save(refreshTokenSaved);

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(tokenPair.getRefreshToken());

        ResponseEntity<ApiResponse<TokenPair>> refreshResponse = restTemplate.exchange(
                "http://localhost:" + port + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(refreshResponse.getBody().getTimestamp()).isNotNull();
        assertThat(refreshResponse.getBody().isSuccess()).isFalse();
        assertThat(refreshResponse.getBody().getData()).isNull();
        assertThat(refreshResponse.getBody().getMessage()).isEqualTo("Refresh token already revoked");
    }

    @Test
    @Order(10)
    @DisplayName("Return Failure as refreshToken is already expired")
    void refreshFailureExpiredToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("password");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        TokenPair tokenPair = response.getBody().getData();

        String refreshTokenHashed = TokenHashUtils.hashedToken(tokenPair.getRefreshToken());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHashed);
        assertThat(refreshToken).isPresent();
        RefreshToken refreshTokenSaved = refreshToken.get();
        refreshTokenSaved.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshTokenSaved);

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(tokenPair.getRefreshToken());

        ResponseEntity<ApiResponse<TokenPair>> refreshResponse = restTemplate.exchange(
                "http://localhost:" + port + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(refreshResponse.getBody().getTimestamp()).isNotNull();
        assertThat(refreshResponse.getBody().isSuccess()).isFalse();
        assertThat(refreshResponse.getBody().getData()).isNull();
        assertThat(refreshResponse.getBody().getMessage()).isEqualTo("Refresh Token expired");
    }

    @Test
    @Order(11)
    @DisplayName("Return Failure when refreshToken is not valid")
    void refreshFailure() {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid refresh token");
    }



    @Test
    @Order(12)
    @DisplayName("Return Success when logout is done successfully")
    void logoutSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tester");
        loginRequest.setPassword("password");

        ResponseEntity<ApiResponse<TokenPair>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        TokenPair tokenPair = response.getBody().getData();

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(tokenPair.getRefreshToken());

        ResponseEntity<ApiResponse<Void>> logoutResponse = restTemplate.exchange(
                "http://localhost:" + port + "/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logoutResponse.getBody()).isNotNull();
        assertThat(logoutResponse.getBody().getCorrelationId()).isNotNull();
        assertThat(logoutResponse.getBody().getTimestamp()).isNotNull();
        assertThat(logoutResponse.getBody().isSuccess()).isTrue();
        assertThat(logoutResponse.getBody().getData()).isNull();

        String refreshTokenHashed = TokenHashUtils.hashedToken(refreshRequest.getRefreshToken());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(refreshTokenHashed);
        assertThat(refreshToken).isPresent();
        RefreshToken savedRefreshToken = refreshToken.get();
        assertThat(savedRefreshToken.isRevoked()).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Return Failure when logout refreshToken is not valid")
    void logoutFailure() {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                "http://localhost:" + port + "/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>(){}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid refresh token");
    }

    @Test
    @Order(14)
    @DisplayName("Return Not Found when auth resource does not exist")
    void missingResourceReturnsNotFound() {
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                "http://localhost:" + port + "/missing",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    }
}
