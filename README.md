# AcctAtlas Search Service

Full-text search service for AccountabilityAtlas. Provides search, filtering, and autocomplete for video content using PostgreSQL FTS.

## Prerequisites

- **Docker Desktop** (for PostgreSQL and LocalStack)
- **Git**

JDK 21 is managed automatically by the Gradle wrapper via [Foojay Toolchain](https://github.com/gradle/foojay-toolchains) -- no manual JDK installation required.

## Clone and Build

```bash
git clone <repo-url>
cd AcctAtlas-search-service
```

Build the project (downloads JDK 21 automatically on first run):

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build
```

## Local Development

### Start dependencies

```bash
docker-compose up -d
```

This starts PostgreSQL 15 and LocalStack (for SQS). Flyway migrations run automatically when the service starts.

### Run the service

```bash
# Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

The service starts on **http://localhost:8084**.

### Quick API test

```bash
# Health check
curl http://localhost:8084/actuator/health

# Search videos (public endpoint, no auth required)
curl "http://localhost:8084/search?q=first+amendment"

# Search with filters
curl "http://localhost:8084/search?q=police&amendments=FIRST,FOURTH&state=TX"
```

### Run tests

```bash
./gradlew test
```

Integration tests use [TestContainers](https://testcontainers.com/) to spin up PostgreSQL automatically -- Docker must be running.

### Code formatting

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using Google Java Format.

```bash
# Check formatting
./gradlew spotlessCheck

# Auto-fix formatting
./gradlew spotlessApply
```

### Full quality check

Runs Spotless, Error Prone, tests, and JaCoCo coverage verification (80% minimum):

```bash
./gradlew check
```

## Docker Image

Build a Docker image locally using [Jib](https://github.com/GoogleContainerTools/jib) (no Dockerfile needed):

```bash
./gradlew jibDockerBuild
```

Build and start the full stack (service + Postgres + LocalStack) in Docker:

```bash
./gradlew composeUp
```

## Project Structure

```
src/main/java/com/accountabilityatlas/searchservice/
  config/        Spring configuration (Security)
  client/        HTTP client for video-service
  domain/        JPA entities (SearchVideo) and enums
  event/         Spring Cloud Stream handlers
  repository/    Spring Data JPA repositories with FTS queries
  service/       Business logic (IndexingService, SearchService)
  web/           REST controllers

src/main/resources/
  application.yml          Shared config
  application-local.yml    Local dev overrides
  db/migration/            Flyway SQL migrations

src/test/java/.../
  client/        Client unit tests
  event/         Event handler tests
  service/       Service unit tests (Mockito)
  web/           Controller tests (@WebMvcTest)
  integration/   Integration tests (TestContainers)
```

API interfaces and DTOs are generated from `docs/api-specification.yaml` by the OpenAPI Generator plugin into `build/generated/`.

## Key Gradle Tasks

| Task | Description |
|------|-------------|
| `bootRun` | Run the service locally (uses `local` profile) |
| `test` | Run all tests |
| `unitTest` | Run unit tests only (no Docker required) |
| `integrationTest` | Run integration tests only (requires Docker) |
| `check` | Full quality gate (format + analysis + tests + coverage) |
| `spotlessApply` | Auto-fix code formatting |
| `jibDockerBuild` | Build Docker image |
| `composeUp` | Build image + docker-compose up |
| `composeDown` | Stop docker-compose services |

## Documentation

- [Technical Overview](docs/technical.md)
- [API Specification](docs/api-specification.yaml) (OpenAPI 3.1)
