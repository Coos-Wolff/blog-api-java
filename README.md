# blog-api

A blog REST API built in Java 25 / Spring Boot 4.1 — the third implementation of the same spec, previously built in [Flask](https://github.com/Coos-Wolff/blog-api) and [FastAPI](https://github.com/Coos-Wolff/blog-api-fast-api).

## Tech stack

- Java 25 (LTS)
- Spring Boot 4.1.0 / Spring Framework 7
- Spring Security 7
- Hibernate 7 via Spring Data JPA
- PostgreSQL 17
- Flyway migrations
- Maven (wrapper committed, use `./mvnw`)
- JUnit 5 (Jupiter) with Mockito and AssertJ for unit tests (Testcontainers-based integration tests planned)

## Architecture

Layered: controller → service → repository.

Domain: `User` and `BlogPost`, one-to-many (a user authors many posts).

## Key features

- JWT auth with separate access and refresh tokens
- Register + login with anti-enumeration defenses
- Ownership-based authorization (author-or-admin)
- CRUD with pagination
- Request validation
- Consistent exception-to-HTTP-status error mapping

## Prerequisites

- JDK 25
- Docker (for PostgreSQL)
- Maven wrapper (no local Maven install needed)

## Getting started

Clone the repo:

```bash
git clone https://github.com/Coos-Wolff/blog-api-java.git
cd blog-api-java
```

Start PostgreSQL via Docker:

```bash
docker compose up -d
```

Flyway migrations run automatically on application startup against `classpath:db/migration` — no separate migration step is required.

Run the app locally using the `local` profile, which supplies dev-only defaults from `application-local.yml`:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Alternatively, supply the required environment variables directly against `application.yml`:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=blog_api
export DB_USERNAME=blog_api
export DB_PASSWORD=blog_api
export JWT_SECRET=change-me
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

## Docker

Build the image:

```bash
docker build -t blog-api-java:0.0.1 .
```

The image is a multi-stage build: a Temurin JDK 25 stage builds the jar, and a Temurin JRE 25 stage runs it. The container runs as a non-root `spring` user, and uses Spring Boot's layered-jar extraction (`jarmode=tools`) so unchanged dependency layers stay cached across builds. The entrypoint runs `app.jar`, a version-independent name the build produces from the Maven artifact via a glob, so the entrypoint doesn't need to change on version bumps.

## Roadmap

Kubernetes manifests are planned for later; not yet included.