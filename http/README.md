# HTTP Client test suites

This directory contains IntelliJ HTTP Client request files (`auth.http`, `posts.http`,
`actuator.http`) for manually exercising the blog-api's auth, blog-post, and actuator endpoints
against a real running instance. Every
request carries a response-handler script (`> {% client.test(...) %}`) that asserts the status
code (and, where documented, response-body fields) the request's own comment says to expect, so a
run is self-verifying — pass/fail, not just "look at the response."

**These hit real HTTP endpoints.** Before running either file, the app must be up with
`SPRING_PROFILES_ACTIVE=local` and Postgres running (`docker compose up -d` from the repo root,
then start the app). Nothing here mocks anything.

## Files

- `http-client.env.json` — public environment file, defines `baseUrl` for the `local` environment.
  Safe to commit.
- `http-client.private.env.json` — private environment file for secrets. Currently empty (an empty
  `local` object) since no secrets are needed yet. **Gitignored** — never commit real values here.
- `auth.http` — register/login/refresh concerns: success paths, duplicate-email 409, validation
  400, wrong-password 401, refresh-token misuse and malformed-token 401s.
- `posts.http` — blog-post CRUD concerns: public list/get, auth-required create, validation 400,
  owner patch/delete, non-owner 403 on patch/delete, and patch-on-missing-post 404. Each concern
  registers and logs in its own user(s) so it can run independently of the others.
- `actuator.http` — actuator health concerns: confirms `/actuator/health`,
  `/actuator/health/liveness`, and `/actuator/health/readiness` are all reachable without a token
  (required for a Kubernetes `livenessProbe`/`readinessProbe` in front of the JWT filter chain),
  and that the readiness group includes the `db` indicator. Run against the `local` profile, whose
  `show-details: always` override means component details ARE visible on the unauthenticated
  top-level health call — production's `show-details: when_authorized` hiding behavior is not
  exercised by this suite (see the NOTE in the file itself).

## Running in the IntelliJ HTTP Client

1. Open `auth.http`, `posts.http`, or `actuator.http` in IntelliJ.
2. Pick **local** from the environment dropdown in the editor gutter/toolbar (this resolves
   `{{baseUrl}}` from `http-client.env.json`).
3. Run requests individually (the ▷ gutter icon next to each request), or use **Run All Requests
   in File** to execute a whole suite top to bottom.
4. Results — including the `client.test`/`client.assert` outcomes — show up in the **Services**
   tool window's **Tests** tab. A concern that depends on an earlier request in the same file (e.g.
   a `PATCH` that needs a post id from a preceding `# @name createPost`) relies on the IDE's
   cross-request response chaining (`{{createPost.response.body.id}}`), which only resolves
   correctly when the requests run in order within the same session — running the whole file top to
   bottom (or at least the requests within one concern, in order) is what makes that work.

## Known limitation: no CLI runner

We evaluated running these suites headlessly with `ijhttp` (JetBrains' standalone HTTP Client CLI)
for CI/scripted use, but ruled it out: the CLI (build 2026.1) does not resolve cross-request
`{{name.response.body.field}}` references — the mechanism these files use to pass a login's token or
a created post's id into later requests in the same concern. Every chained concern (i.e. anything
beyond a single standalone request) fails under the CLI for that reason alone, even though the
underlying endpoints behave correctly (verified independently via `curl`). These suites are
IDE-only for now — there's no `ijhttp` invocation or generated report to run or check in.

Automated, CI-runnable HTTP/security coverage will come from the planned Testcontainers integration
tests (see `CLAUDE.md`), not from this CLI path.