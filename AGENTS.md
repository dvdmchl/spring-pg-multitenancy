# spring-pg-multitenancy – instructions for AI agents

Shared instructions for all coding agents (Codex reads this file directly, Claude Code imports it via `CLAUDE.md`).
Keep this file the single source of truth; do not duplicate rules into agent-specific files.

## Project overview

Schema-per-tenant multitenancy library for Spring Boot 4 and PostgreSQL (`org.dreamabout.sw:spring-pg-multitenancy`).
Each tenant lives in its own PostgreSQL schema; the library switches Hibernate's tenant and the connection
`search_path` for the current request, and manages the tenant schema lifecycle. Other databases are not supported.

Extracted from [FRP](https://github.com/dvdmchl/frp), which is its main consumer. User-facing documentation is in
`README.md`; keep it in sync with behaviour and configuration changes.

- Java 22, Spring Boot 4.0.x (exact versions in `pom.xml`), Hibernate, optional Flyway, Lombok
- Tests: JUnit 5, Spring Boot test, Testcontainers (PostgreSQL)

## Structure (`src/main/java/org/dreamabout/sw/multitenancy`)

- `core` – public API: `@Multitenant`, `TenantResolver`, `TenantIdentifier`, `TenantContext`,
  `MultitenancyTaskDecorator`
- `config` – auto-configuration (`MultitenancyAutoConfiguration`, `TenantSchemaAutoConfiguration`,
  registered in `META-INF/spring/...AutoConfiguration.imports`) and `MultitenancyProperties` (`multitenancy.*`)
- `aop` – `SchemaSearchPathAspect` setting `search_path` for `@Multitenant`
- `hibernate` – multi-tenant connection provider and tenant identifier resolver
- `schema` – `TenantSchemaManager` (create/copy/drop/orphans), `TenantSchemaMigrator` (Flyway implementation),
  `TenantRegistry`, `TableCopyPriorityProvider`, `SchemaNames`, events in `schema/event`
- `web` – request filter and interceptor

## Workflow rules

- **Single developer.** No pull requests and no feature branches unless the user explicitly asks.
  Work directly on `main`.
- **Commits.** Message format `#<issue> - <description>`, issues in dvdmchl/spring-pg-multitenancy
  (FRP issue numbers are fine when the change is driven by FRP). Agents may commit and push to `main` themselves
  once tests pass. Commit only your own changes; never revert, stash or discard work you did not make.
- **Review before commit.** Before every commit, review your own diff and fix the findings, then rerun the
  relevant tests. Claude Code runs the `/code-review` skill, Codex `/review`; without such a tool, go through the
  diff yourself (bugs, missed edge cases, duplication, rules in this file).
- **Language.** The repository is public and may get more contributors, so everything in it and around it is in
  English: issues and issue comments, commit messages, code and code comments, README and Javadoc. Talk to the user
  (David) in Czech.
- **Issues.** Every new issue in dvdmchl/spring-pg-multitenancy must also be added to the linked GitHub Project
  **Spring PG Multitenant** (https://github.com/users/dvdmchl/projects/22) with a Status. The gh token needs the
  `project` scope (`gh auth refresh -s project`, ask the user to run it).
  ```bash
  gh issue create -R dvdmchl/spring-pg-multitenancy --title "..." --body "..." --project "Spring PG Multitenant"
  ITEM=$(gh project item-add 22 --owner dvdmchl --url <issue-url> --format json -q .id)   # idempotent, returns item id
  gh project item-edit --project-id PVT_kwHOATHpmc4Blc0s --id $ITEM \
    --field-id PVTSSF_lAHOATHpmc4Blc0szhkJqy8 --single-select-option-id <status>
  ```
  Status option ids: Backlog `f75ad846`, In progress `47fc9ee4`, Done `98236657`. Set In progress when you
  start the work and Done when you close the issue. Issues for FRP itself go to dvdmchl/frp and its project
  FRP (https://github.com/users/dvdmchl/projects/10).
- Close the issue when the work is done and pushed.
- Never run `mvn -Prelease deploy` (Maven Central release) unless the user explicitly asks.

## Environment (Windows + WSL)

- Docker runs in WSL; prefix Docker commands with `wsl`. Testcontainers reaches it via `DOCKER_HOST` or
  `~/.testcontainers.properties` (`docker.host=tcp://<wsl-ip>:2375`), so `mvn test` runs the integration tests.
- Stop only containers you started yourself; never remove the user's containers or volumes.

## Build and test

```bash
mvn test       # unit + integration tests (Testcontainers)
mvn verify     # + Checkstyle, SpotBugs and the JaCoCo coverage gate (80 % lines)
mvn install    # install into the local .m2 – required before FRP picks up changes
mvn verify -Psonar sonar:sonar   # Sonar analysis (needs the sonarqube container and SONAR_TOKEN)
```

- **Static analysis.** Checkstyle (`checkstyle.xml`, same rules as FRP, runs in `validate`), SpotBugs (threshold
  Medium, `process-classes`) and the JaCoCo line coverage gate (80 %, `verify`) fail the build on any violation.
  Fix the code, do not fight or suppress the tools. CI (`.github/workflows/ci.yml`) runs `mvn verify`.
- **Sonar.** Run the Sonar analysis before finishing every change: `mvn verify -Psonar sonar:sonar` against the local
  SonarQube (`http://localhost:9000`, the `sonarqube` container from FRP's `docker-compose.override.yml`, started with
  `wsl docker compose up -d` there), project `spring-pg-multitenancy`, coverage from JaCoCo
  (`target/site/jacoco/jacoco.xml`). `SONAR_TOKEN` must be set in the environment; if it is missing, ask the user for
  a token instead of skipping the scan. The quality gate must pass and new bugs, vulnerabilities, code smells,
  security hotspots and duplications must be fixed (`/api/issues/search?componentKeys=spring-pg-multitenancy`).

The library is not on Maven Central yet; FRP (`C:\dev\Projects\office\frp\FamilyResourcePlanning-FRP`) uses the
`0.1.0-SNAPSHOT` from the local `.m2`. After changing the library, run `mvn install` and then build/test FRP
(`mvn test -pl code/backend`) to make sure the consumer still works.

## Engineering practices

These apply to all code and tests and complement the conventions below.

- **Test-first (TDD).** For new behaviour write a failing test first, then the minimal code that makes it pass, then
  refactor with the tests green. A bug fix starts with a test that reproduces the bug; keep it as a regression test.
- **Test quality.** One behaviour per test, descriptive names (`shouldXWhenY`), Arrange-Act-Assert structure.
  Tests are deterministic: no `Thread.sleep` or fixed timeouts, no dependence on test order, current time or locale.
  Mock only boundaries you do not own; behaviour touching PostgreSQL is tested against the real database.
- **DRY, but not prematurely.** Do not copy logic, constants, SQL or test setup; extract a shared method, helper or
  fixture instead. Introduce an abstraction when a third copy would appear, not speculatively.
- **KISS / YAGNI.** Implement only what is needed: no speculative options, extension points, unused parameters or
  "just in case" code. Every new public type or property is API that FRP and others may depend on.
- **Clean code.** Intention-revealing names; small, focused methods and classes; early returns instead of deep
  nesting. No dead or commented-out code, no `System.out`/`printStackTrace` (log through SLF4J), no `TODO` without
  an issue number. Never return `null` for collections or `Optional`. Prefer immutability (`final`, records,
  unmodifiable collections).
- **Scope.** Keep changes focused on the issue. Unrelated refactoring or cleanup goes into its own issue and commit.
- **Dependencies.** Do not add a new library or Maven plugin without the user's approval; keep the library's
  dependency footprint small (prefer `optional`/`provided` scope for integrations).
- **Security.** No secrets in the repository, tests or logs. SQL only with bind parameters; identifiers only through
  `SchemaNames` (see Conventions).

## Conventions

- Keep the library generic: no FRP-specific concepts (users, ownership, access rights) – those belong in FRP.
- Public API changes need care: FRP depends on `core` and `schema`; update FRP together with a breaking change.
- Configuration goes through `MultitenancyProperties` (`multitenancy.*`) with sensible defaults; document new
  properties in `README.md`.
- Validate schema names through `SchemaNames` before using them in SQL; never concatenate unvalidated identifiers.
- Use `JdbcTemplate` so SQL joins the Spring transaction; do not call `dataSource.getConnection()` directly.
- Every new feature or fix comes with tests, written first (TDD); behaviour touching PostgreSQL gets an integration test against a real
  database (Testcontainers). No disabled or placeholder tests; every test has meaningful assertions.

## Definition of done

- Tests written first cover the new behaviour; coverage does not drop.
- `mvn verify` passes (static analysis and coverage gate); the Sonar scan passes its quality gate with no open
  issues; after `mvn install` the FRP backend still builds and its tests pass (`mvn test -pl code/backend`).
- `README.md` updated when behaviour, configuration or the public API changed; public API (`core`, `schema`) has Javadoc.
- Committed as `#<issue> - <description>`, pushed to `main`, the issue closed and its project Status set to Done.
