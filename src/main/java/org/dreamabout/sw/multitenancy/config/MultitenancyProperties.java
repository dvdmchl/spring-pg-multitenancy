package org.dreamabout.sw.multitenancy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "multitenancy")
public class MultitenancyProperties {
    /**
     * The name of the default/public schema.
     */
    private String defaultSchema = "public";

    /**
     * Schemas that are never treated as tenant schemas: not reported as orphans and never dropped.
     */
    private List<String> excludedSchemas = new ArrayList<>();

    private Migration migration = new Migration();

    @Data
    public static class Migration {
        /**
         * Flyway locations of the migrations applied to every tenant schema.
         */
        private List<String> locations = new ArrayList<>(List.of("classpath:db/migration/tenant"));

        /**
         * Migrate the schemas of all tenants from the TenantRegistry on startup.
         */
        private boolean runOnStartup = true;

        /**
         * Name of the Flyway schema history table in every tenant schema.
         */
        private String table = "flyway_schema_history";

        /**
         * Clean the tenant schema before migrating it. Destroys all data, for development only.
         */
        private boolean cleanBeforeMigrate = false;

        /**
         * Run Flyway repair and migrate again when validation fails.
         */
        private boolean repairOnValidationError = true;
    }
}
