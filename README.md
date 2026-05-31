# Integrator

Integrator is a Spring Boot microservices platform for secure authentication, dynamic route management, gateway-based request routing, JSON transformation, observability, and event-driven integrations.

## Modules

- `integrator-auth-service` - user registration, login, JWT access tokens, and refresh tokens
- `integrator-route-service` - route and routing rule management
- `integrator-gateway` - Spring Cloud Gateway service for dynamic request routing and route refresh
- `integrator-common` - shared API, exception, event, and observability utilities

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Flyway
- Apache Kafka
- Testcontainers
- Maven
- JaCoCo
- SonarCloud

## CI

GitHub Actions runs tests, generates JaCoCo coverage, uploads test reports, and publishes SonarCloud analysis on updates to `develop` and `main`.

Workflow:

```text
.github/workflows/ci.yml
```