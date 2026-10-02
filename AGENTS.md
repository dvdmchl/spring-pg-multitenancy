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
- Close the issue when the work is done and pushed.
- Never run `mvn -Prelease deploy` (Maven Central release) unless the user explicitly asks.

## Environment (Windows + WSL)

- Docker runs in WSL; prefix Docker commands with `wsl`. Testcontainers reaches it via `DOCKER_HOST` or
  `~/.testcontainers.properties` (`docker.host=tcp://<wsl-ip>:2375`), so `mvn test` runs the integration tests.
- Stop only containers you started yourself; never remove the user's containers or volumes.

## Build and test

```bash
mvn test       # unit + integration tests (Testcontainers)
mvn install    # install into the local .m2 – required before FRP picks up changes
```

The library is not on Maven Central yet; FRP (`C:\dev\Projects\office\frp\FamilyResourcePlanning-FRP`) uses the
`0.1.0-SNAPSHOT` from the local `.m2`. After changing the library, run `mvn install` and then build/test FRP
(`mvn test -pl code/backend`) to make sure the consumer still works.

## Conventions

- Keep the library generic: no FRP-specific concepts (users, ownership, access rights) – those belong in FRP.
- Public API changes need care: FRP depends on `core` and `schema`; update FRP together with a breaking change.
- Configuration goes through `MultitenancyProperties` (`multitenancy.*`) with sensible defaults; document new
  properties in `README.md`.
- Validate schema names through `SchemaNames` before using them in SQL; never concatenate unvalidated identifiers.
- Use `JdbcTemplate` so SQL joins the Spring transaction; do not call `dataSource.getConnection()` directly.
- Every new feature or fix comes with tests; behaviour touching PostgreSQL gets an integration test against a real
  database (Testcontainers). No disabled or placeholder tests; every test has meaningful assertions.
