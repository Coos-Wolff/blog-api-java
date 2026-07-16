# CLAUDE.md

Working conventions for this repo.

## Working style

- The user writes application code. Claude reviews, explains, and flags issues — especially duplicated code, where Claude should propose the idiomatic refactor (shared helpers, dependency factories, parameterization) rather than just pointing out the duplication.
- Claude may scaffold infrastructure/config more directly (build files, CI, docker, migrations skeletons, etc.).

## Stack currency

Boot 4.1, Security 7, Hibernate 7, and Java 25 are newer than Claude's training cutoff. Verify current idioms via web search rather than reproducing older patterns from memory — especially:
- Spring Security 7 configuration
- JWT / filter chain setup
- Anything else version-sensitive

## Schema and persistence

- Schema is owned by Flyway. Hibernate runs with `ddl-auto=validate` and never generates or modifies schema.
- Primary keys are database-generated UUIDs (`gen_random_uuid()` in Postgres), mapped with Hibernate's `@Generated(event = INSERT)` + `@ColumnDefault`, not `@GeneratedValue`.

## Configuration

- Config via Spring profiles and env-var placeholders in `application.yml`.
- Local dev values live in `application-local.yml`, which is gitignored and never committed.
- No `.env` files.

## Branch workflow

- All changes go through a feature branch → PR → merge to `main`.
- `main` is protected; no direct pushes.

## Build

- Maven, always via the wrapper: `./mvnw`.

## Entity conventions

- Entities are plain classes using scoped Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor(access = PROTECTED)`); never `@Data`. DTOs and value objects are Java records.
- Primary keys are database-generated UUIDs (Postgres `gen_random_uuid()`), mapped with `@Generated(event = EventType.INSERT)` + `@ColumnDefault("gen_random_uuid()")` and `@Column(updatable = false, nullable = false)`. Never `@GeneratedValue`. The same `@Generated` + `@ColumnDefault` pattern is used for any other DB-owned default (e.g. `is_admin` defaults to `false` in the DB). Note `@ColumnDefault` alone only affects generated DDL; `@Generated` is what makes Hibernate omit the column on insert and read the DB value back at runtime.
- `equals`/`hashCode` are hand-written and id-based: `equals` returns `false` when `id` is null and uses an `instanceof` pattern (proxy-safe); `hashCode` returns a stable class-based constant (`getClass().hashCode()`).
- Bidirectional associations: the `@ManyToOne` side is owning and fetched `LAZY`; the `@OneToMany` inverse side uses `mappedBy`, is a `Set` initialized to `new HashSet<>()`, is protected with `@Setter(AccessLevel.NONE)`, and is mutated only through `addX`/`removeX` sync helpers that keep both sides consistent.
- User deletion cascades to that user's posts: DB-level `ON DELETE CASCADE` on the `author_id` FK plus Hibernate `cascade = ALL` + `orphanRemoval = true` and `@OnDelete(action = CASCADE)`. Deliberate divergence from the FastAPI build, which used `NO ACTION` (blocked deleting a user with posts).
- Schema is owned by Flyway; Hibernate runs `ddl-auto=validate` and never generates schema. Local Postgres runs via `docker-compose.yml` (Postgres 17, pinned deliberately to avoid the Flyway "Unsupported Database: PostgreSQL 18" issue and because `gen_random_uuid()` is in core).
- Java 25 requires Lombok declared explicitly in `maven-compiler-plugin` `annotationProcessorPaths` (javac 23+ no longer runs annotation processors from the classpath by default).

## Authentication and error handling

- Authentication uses Spring Security OAuth2 Resource Server with self-issued, HMAC-signed (HS256) JWTs. Tokens are encoded/decoded with Spring's own `NimbusJwtEncoder`/`JwtDecoder` configured from a symmetric secret; no third-party JWT library is used.
- Access vs refresh tokens are distinguished by a `token_type` claim (`"access"`/`"refresh"`). Access tokens also carry a `roles` claim; refresh tokens are minimal (no roles). The refresh endpoint validates the token by decoding it and checking `token_type == "refresh"`.
- The design is stateless with no server-side token store: there is no revocation or refresh-token rotation. The refresh token's expiry is a hard session limit; once it expires the user must log in again (which issues fresh tokens). Login always issues new tokens; older tokens remain valid until their own expiry. A token store enabling rotation/revocation/logout is a possible future enhancement.
- Anti-enumeration: login relies on `DaoAuthenticationProvider` defaults (`hideUserNotFoundExceptions` and the internal dummy-password timing defense, both left on). `AuthService.login` catches `AuthenticationException` and rethrows a single `InvalidCredentialsException` with a uniform "Invalid credentials" message, so unknown-email and wrong-password are indistinguishable (identical 401).
- Error handling uses RFC 9457 `ProblemDetail` via a single global `@RestControllerAdvice` (`GlobalExceptionHandler`) that extends `ResponseEntityExceptionHandler` (so built-in MVC/validation exceptions render as `ProblemDetail` automatically). Handlers return only controlled messages; raw exception messages and DB errors are never exposed to clients (they are logged server-side). Note: on Spring Framework 7 / Boot 4, `ProblemDetail.type` is not set by default and is omitted from the JSON unless set explicitly.
- Passwords are hashed with `BCryptPasswordEncoder` in the service layer before persistence; entities never perform encoding. Password policy: 12–72 characters (72 is bcrypt's byte limit).
- Package-by-feature: the top-level decomposition is by feature (`auth`, `user`, `blogpost`). Within a feature, supporting-type categories may be sub-packaged (`auth.dto`, `auth.exception`), but core feature classes stay flat — do NOT create layer sub-packages like `service`/`controller`/`repository` inside a feature. The one justified top-level cross-cutting package is `exception` (the global handler), which serves all features.
- Config binds via `@ConfigurationProperties` records discovered by `@ConfigurationPropertiesScan` on the application class. JSON uses a global SNAKE_CASE property-naming strategy (`spring.jackson.property-naming-strategy`), so DTO fields are camelCase in Java and snake_case on the wire.

## Testing

- Unit tests use JUnit 5 (`org.junit.jupiter`) with Mockito via `@ExtendWith(MockitoExtension.class)`; `@Mock` for collaborators, `@InjectMocks` for the class under test where it fits. `MockitoExtension` runs with strict stubbing, so only stub what a test actually exercises.
- Assertions use AssertJ (`assertThat` / `assertThatThrownBy`), not JUnit's built-in assertions.
- Every test method has a `@DisplayName` describing the behavior under test, and follows a given/when/then (Arrange/Act/Assert) structure.
- Tests are written against intended/specified behavior, not to mirror whatever the current implementation happens to do. A test that fails because the code is wrong is a finding to surface, not something to paper over.
- For crypto/serialization logic (e.g. JWT encoding), tests use real collaborators rather than mocks where mocking would only prove "a method was called" rather than that the output is correct — e.g. `TokenServiceTest` builds a real `NimbusJwtEncoder`/`NimbusJwtDecoder` pair with a test secret and asserts on decoded claims.
- Unit tests cover logic-bearing code (token issuance, user-details mapping, and the branching in `AuthService`: duplicate-email guard, encoded-not-raw password on save, uniform `InvalidCredentialsException` on auth failure, and the refresh `token_type`/decode/user-lookup branches). Pure data holders, DTO records, and constants are not unit-tested.
- Controllers, the global exception handler, the security filter chain, and full application context/startup are intentionally NOT unit-tested; they are deferred to integration tests built with Testcontainers (planned, not yet written). The Spring Initializr default context-load test (`BlogApiApplicationTests`) was removed rather than kept, because a proper context-load test belongs in that future Testcontainers integration suite.
- Current coverage boundary: the auth feature is unit-tested at the logic level and manually verified end-to-end via `http/auth.http`, but the wired HTTP/security layer and application startup are not yet covered by automated integration tests.

## BlogPost CRUD

- Public reads (`GET /api/posts`, `GET /api/posts/{id}`) are `permitAll`; writes (`POST`/`PATCH`/`DELETE`) require authentication, enforced by an HTTP-method-specific matcher in `SecurityConfig`.
- Update is `PATCH` with partial semantics: null/omitted fields are left unchanged; each field is applied independently. `UpdateBlogPostRequest` fields are nullable with `@Size` (no `@NotBlank`), so validation only fires on provided values.
- Ownership: modify/delete require the current user to be the post's author OR an admin, enforced in the service layer (`BlogPostService.assertCanModify`) by loading the post then comparing author id / checking admin authority — not via `@PreAuthorize`, because the decision needs the loaded entity. Author on create comes from the JWT subject, never the request body.
- The access token's `roles` claim is mapped to Spring authorities via a `JwtAuthenticationConverter` + `JwtGrantedAuthoritiesConverter` in `SecurityConfig` (claim name `"roles"`, empty authority prefix since values already carry `ROLE_`). Role values live in the `Roles` constants class.
- `CurrentUserProvider` reads the authenticated user's id (JWT subject) and admin status (authorities) from the `SecurityContext`; it throws `IllegalStateException` if called without a JWT principal (a should-never-happen guard on write paths).
- Responses: `BlogPostResponse` nests an `AuthorResponse` (id + name only — never email). `PagedResponse<T>` wraps Spring `Page` data (content, page, size, totalElements, totalPages, hasNext, hasPrevious; 0-based page numbers) via a static `from(Page)` factory. List queries use `@EntityGraph(attributePaths="author")` to avoid N+1 (safe with pagination because author is `@ManyToOne`, not a collection).
- Create returns 201 with a `Location` header pointing at the new resource. Delete returns 204.
- Entity-to-DTO mapping is hand-written in the service (MapStruct deliberately deferred as a future exercise). Entities never import DTOs; DTOs never import entities.
- HTTP test suites (`http/auth.http`, `http/posts.http`) carry `client.test`/`client.assert` response-handler assertions so a run is self-verifying, and are run in the IntelliJ HTTP Client (IDE). They cover the HTTP/security wiring (public reads, auth-required writes, ownership 403s) that unit tests do not. The `ijhttp` CLI was evaluated but does not resolve cross-request references, so the IDE is the supported way to run chained concerns; automated HTTP-level coverage will come from the planned Testcontainers integration tests. The admin-override path is not manually testable via `/api/auth/register` (which only creates non-admins) and is covered by unit tests until integration tests exist.
