package com.integrator.auth;

import com.integrator.auth.repository.RefreshTokenRepository;
import com.integrator.auth.repository.UserRepository;
import com.integrator.auth.service.JwtService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class AbstractContainerBaseTest {

    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_EMAIL = "admin@integrator.dev";
    static final String ADMIN_PASSWORD = "password";

    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:latest")
            .withDatabaseName("integrator")
            .withUsername("auth_svc")
            .withPassword("auth_svc");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "auth");
        registry.add("spring.flyway.schemas", ()->"auth");
        registry.add("bootstrap.admin.enabled", () -> "true");
        registry.add("bootstrap.admin.username", () -> ADMIN_USERNAME);
        registry.add("bootstrap.admin.email", () -> ADMIN_EMAIL);
        registry.add("bootstrap.admin.password", () -> ADMIN_PASSWORD);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder passwordEncoder;
}
