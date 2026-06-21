# Integrator

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=sreesankar711_integrator&metric=bugs)](https://sonarcloud.io/summary/new_code?id=sreesankar711_integrator)

Integrator is a Spring Boot microservices platform for secure authentication, dynamic route management, gateway-based routing, rate limiting, observability, event-driven integrations, and MCP-based route inspection.
## Modules

- `integrator-auth-service` - user registration, login, JWT access tokens, and refresh tokens, and bootstrap admin support
- `integrator-route-service` - route and routing rule management
- `integrator-gateway` - Spring Cloud Gateway service for dynamic request routing and route refresh, rate limiting, and MCP route inspection
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

## Docker Setup

Start core infrastructure:

```bash
docker compose -f compose.yaml up -d
```

Start core infrastructure with observability:

```bash
docker compose -f compose.yaml -f compose.observability.yaml up -d
```

Useful local URLs:

```text
Kafka UI: http://localhost:8090
Grafana:  http://localhost:3000
OTLP HTTP: http://localhost:4318
```

Seed demo route data after Route Service migrations have created the `route` tables:

```bash
docker cp ./integrator-data.sql postgres:/tmp/integrator-data.sql
docker exec -it postgres psql -U postgres -d integrator -f /tmp/integrator-data.sql
```

## Gateway MCP

`integrator-gateway` exposes a stateless MCP server at:

```text
http://localhost:8080/mcp
```

MCP config example:

```json
{
  "mcpServers": {
    "integrator-gateway": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

Exposed MCP tools:

```text
list_all_routes
get_route
list_runtime_routes
get_runtime_route
```

Exposed MCP resources:

```text
integrator://routes
integrator://runtime-routes
```

Route-specific resources are exposed as URI templates:

```text
integrator://routes/{routeId}
integrator://runtime-routes/{routeId}
```