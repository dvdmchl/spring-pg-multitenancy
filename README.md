# spring-pg-multitenancy

Schema-per-tenant multitenancy for Spring Boot and **PostgreSQL**. Each tenant lives in its own
PostgreSQL schema; the library switches Hibernate's tenant and the connection `search_path`
for the current request. Other databases are not supported.

Originally extracted from [FRP – Family Resource Planning](https://github.com/dvdmchl/frp).

## Requirements

- Java 22+
- Spring Boot 4
- PostgreSQL

## Installation

The library is not on Maven Central yet. Build and install it into your local repository:

```bash
mvn install
```

Then add the dependency:

```xml
<dependency>
    <groupId>org.dreamabout.sw</groupId>
    <artifactId>spring-pg-multitenancy</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

The library registers itself through Spring Boot auto-configuration
(`MultitenancyAutoConfiguration`). The application has to provide:

1. A `TenantResolver` bean that tells the library which tenant (schema) the current request belongs to:

   ```java
   @Bean
   TenantResolver tenantResolver() {
       return () -> TenantIdentifier.of(currentUserSchema());
   }
   ```

2. `@Multitenant` on the services or methods that should run against the tenant schema.
   The `search_path` is set to `<tenant>, <default schema>` before the call.

Configuration:

```properties
# schema used when no tenant is set (default: public)
multitenancy.default-schema=public
```

For async work, use `MultitenancyTaskDecorator` so the tenant context is propagated to worker threads.

## Tenant schema lifecycle

`TenantSchemaManager` creates, copies and drops tenant schemas:

```java
tenantSchemaManager.createSchema("tenant_a");          // CREATE + tenant migrations
tenantSchemaManager.copySchema("tenant_a", "tenant_b"); // structure by migrations, then data
tenantSchemaManager.dropSchema("tenant_b");
tenantSchemaManager.findOrphanSchemas();               // schemas no tenant owns
```

Tenant schema names must start with a lowercase letter and contain only lowercase letters, digits and
underscores. Ownership and access rights of tenants are up to the application.

**Migrations.** With Flyway on the classpath (`flyway-core` and `flyway-database-postgresql`), every new
schema is migrated from `multitenancy.migration.locations`. Migrations can use the `${schema}` placeholder.
Provide your own `TenantSchemaMigrator` bean to use something else.

**Startup.** When the application provides a `TenantRegistry` bean listing the schemas of all tenants,
all of them are migrated on startup. A failing schema is logged and does not stop the others.

**Copy order.** `TableCopyPriorityProvider` beans decide the order of the copied tables (lower first,
default 10), so that referenced tables are filled first. Sequences are reset after the copy.

**Events.** `TenantSchemaCreatedEvent`, `TenantSchemaCopiedEvent` and `TenantSchemaDroppedEvent` are published
as Spring application events, e.g. for audit logging or seeding tenant data.

```properties
# never reported as orphans or dropped (public and the default schema always are protected)
multitenancy.excluded-schemas=template
multitenancy.migration.locations=classpath:db/migration/tenant
multitenancy.migration.run-on-startup=true
multitenancy.migration.table=flyway_schema_history
# destroys all data, for development only
multitenancy.migration.clean-before-migrate=false
multitenancy.migration.repair-on-validation-error=true
```

## Releasing to Maven Central

The `release` profile attaches sources and javadoc, signs the artifacts with GPG and uploads them
through the Sonatype Central Portal (`autoPublish` is off, so the deployment has to be confirmed in the portal):

```bash
mvn -Prelease deploy
```

It needs a verified `org.dreamabout` namespace, a GPG key and a `central` server entry with the
portal token in `~/.m2/settings.xml`.

## License

[GNU Affero General Public License v3.0](LICENSE)
