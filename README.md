# search-service

Full-text search service for AccountabilityAtlas. Provides search, filtering, and autocomplete for video content.

## Prerequisites

- Java 21
- Docker (for PostgreSQL and LocalStack in local development)
- Gradle 9.x (uses wrapper)

## Clone & Build

```bash
git clone <repo-url>
cd AcctAtlas-search-service
./gradlew build
```

## Local Development

```bash
# Start dependencies (from parent directory)
cd .. && docker-compose up -d postgres localstack

# Run service
./gradlew bootRun

# Service available at http://localhost:8084
```

## Docker Image

```bash
./gradlew jibDockerBuild
```

## Project Structure

```
src/main/java/com/accountabilityatlas/searchservice/
├── SearchServiceApplication.java
├── config/          # Security, web config
├── domain/          # Entities and enums
├── event/           # Spring Cloud Stream handlers
├── repository/      # Data access
├── service/         # Business logic
└── web/             # REST controllers
```

## Key Gradle Tasks

| Task | Description |
|------|-------------|
| `bootRun` | Run locally with local profile |
| `test` | Run all tests |
| `unitTest` | Run unit tests only (no Docker) |
| `integrationTest` | Run integration tests (requires Docker) |
| `check` | Full quality gate (tests + coverage) |
| `spotlessApply` | Fix code formatting |
| `jibDockerBuild` | Build Docker image |
| `composeUp` | Build image + start docker-compose |
| `composeDown` | Stop docker-compose services |

## Documentation

- [Technical Documentation](docs/technical.md)
- [API Specification](docs/api-specification.yaml)
