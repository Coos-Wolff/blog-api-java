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
