package org.dreamabout.sw.multitenancy.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Runs Flyway migrations from {@code multitenancy.migration.locations} against a single tenant schema.
 * The migrations can refer to the schema with the {@code ${schema}} placeholder.
 */
@RequiredArgsConstructor
@Slf4j
public class FlywayTenantSchemaMigrator implements TenantSchemaMigrator {

    public static final String SCHEMA_PLACEHOLDER = "schema";

    private final DataSource dataSource;
    private final MultitenancyProperties properties;

    @Override
    public void migrate(String schemaName) {
        log.info("Running migrations for schema: {}", schemaName);
        var migration = properties.getMigration();
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .createSchemas(true)
                .table(migration.getTable())
                .cleanDisabled(!migration.isCleanBeforeMigrate())
                .locations(migration.getLocations().toArray(String[]::new))
                .placeholders(Map.of(SCHEMA_PLACEHOLDER, schemaName))
                .load();

        if (migration.isCleanBeforeMigrate()) {
            log.warn("Cleaning schema {} before migration (multitenancy.migration.clean-before-migrate=true)", schemaName);
            flyway.clean();
        }

        try {
            flyway.migrate();
        } catch (FlywayValidateException e) {
            if (!migration.isRepairOnValidationError()) {
                throw e;
            }
            log.warn("Schema {} validation failed, attempting repair", schemaName);
            flyway.repair();
            flyway.migrate();
        }
    }
}
