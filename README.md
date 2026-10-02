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
