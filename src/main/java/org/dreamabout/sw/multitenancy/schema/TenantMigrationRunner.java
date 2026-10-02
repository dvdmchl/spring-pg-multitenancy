package org.dreamabout.sw.multitenancy.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Migrates the schemas of all tenants from the {@link TenantRegistry} on startup.
 * A failing schema is logged and does not stop the others.
 */
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationRunner implements ApplicationRunner {

    private final TenantRegistry tenantRegistry;
    private final TenantSchemaMigrator migrator;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting migration of tenant schemas...");
        var schemas = tenantRegistry.getTenantSchemas();
        var failed = 0;
        for (var schema : schemas) {
            try {
                migrator.migrate(schema);
            } catch (Exception e) {
                failed++;
                log.error("Failed to migrate schema: {}", schema, e);
            }
        }
        log.info("Tenant schema migration finished. Migrated {} of {} schemas.", schemas.size() - failed, schemas.size());
    }
}
