# Integrator

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=bugs)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)

Integrator is a Spring Boot microservices platform for secure authentication, dynamic route management, gateway-based routing, rate limiting, observability, and event-driven integrations.

## Modules

- `integrator-auth-service` - user registration, login, JWT access tokens, and refresh tokens, and bootstrap admin support
- `integrator-route-service` - route and routing rule management
- `integrator-gateway` - Spring Cloud Gateway service for dynamic request routing and route refresh
- `integrator-common` - shared API, exception, event, and observability utilities

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Flyway
- Apache Kafka
- Testcontainers
- Redis
- Micrometer Prometheus
- OpenTelemetry
- Maven
- JaCoCo
- SonarCloud

## CI

GitHub Actions runs tests, generates JaCoCo coverage, uploads test reports, and publishes SonarCloud analysis on updates to `develop` and `main`.

Workflow:

```text
.github/workflows/ci.yml
```
