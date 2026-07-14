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
