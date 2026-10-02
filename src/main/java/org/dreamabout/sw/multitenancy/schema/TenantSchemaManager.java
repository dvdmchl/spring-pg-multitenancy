package org.dreamabout.sw.multitenancy.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCopiedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCreatedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaDroppedEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Creates, copies and drops tenant schemas and publishes
 * {@link TenantSchemaCreatedEvent}, {@link TenantSchemaCopiedEvent} and {@link TenantSchemaDroppedEvent}.
 * <p>
 * Tenant metadata (ownership, access rights) is up to the application.
 */
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaManager {

    private static final String SELECT_SCHEMAS = """
            SELECT nspname FROM pg_namespace
            WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'
            ORDER BY nspname
            """;

    private static final String SELECT_SCHEMA_EXISTS = "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)";

    private static final String SELECT_TABLES = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> ?
            """;

    /**
     * Resets every sequence owned by a column of the schema to MAX(column) + 1.
     * The schema name is validated by {@link SchemaNames#validate(String)} before it is inserted.
     */
    private static final String UPDATE_SEQUENCES = """
            DO $$
            DECLARE
                r RECORD;
            BEGIN
                FOR r IN
                    SELECT
                        s.nspname AS table_schema,
                        t.relname AS table_name,
                        a.attname AS column_name,
                        c.relname AS sequence_name
                    FROM pg_class c
                    JOIN pg_namespace s ON s.oid = c.relnamespace
                    JOIN pg_depend d ON d.objid = c.oid AND d.deptype IN ('a', 'i')
                    JOIN pg_attribute a ON a.attrelid = d.refobjid AND a.attnum = d.refobjsubid
                    JOIN pg_class t ON t.oid = d.refobjid
                    WHERE c.relkind = 'S'
                      AND s.nspname = '%s'
                LOOP
                    EXECUTE format('SELECT setval(%%L, (SELECT COALESCE(MAX(%%I), 0) + 1 FROM %%I.%%I), false)',
                                   quote_ident(r.table_schema) || '.' || quote_ident(r.sequence_name),
                                   r.column_name,
                                   r.table_schema,
                                   r.table_name);
                END LOOP;
            END $$;
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MultitenancyProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<TenantSchemaMigrator> migrator;
    private final ObjectProvider<TenantRegistry> tenantRegistry;
    private final ObjectProvider<TableCopyPriorityProvider> priorityProviders;

    public boolean schemaExists(String schemaName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(SELECT_SCHEMA_EXISTS, Boolean.class, schemaName));
    }

    /**
     * Creates the schema and runs the tenant migrations on it. Without a {@link TenantSchemaMigrator}
     * (e.g. Flyway is not on the classpath) an empty schema is created.
     */
    public void createSchema(String schemaName) {
        SchemaNames.validate(schemaName);
        requireNotExists(schemaName);

        log.info("Creating tenant schema: {}", schemaName);
        var schemaMigrator = migrator.getIfAvailable();
        if (schemaMigrator != null) {
            schemaMigrator.migrate(schemaName);
        } else {
            jdbcTemplate.execute("CREATE SCHEMA " + SchemaNames.quote(schemaName));
        }

        eventPublisher.publishEvent(new TenantSchemaCreatedEvent(schemaName));
    }

    /**
     * Creates the target schema by the tenant migrations and copies the data of all tables from the source.
     * Tables are copied in the order given by the {@link TableCopyPriorityProvider}s, sequences are reset afterwards.
     */
    @Transactional
    public void copySchema(String sourceSchemaName, String targetSchemaName) {
        SchemaNames.validate(targetSchemaName);
        if (!schemaExists(sourceSchemaName)) {
            throw new IllegalArgumentException("Source schema not found: " + sourceSchemaName);
        }
        requireNotExists(targetSchemaName);
        var schemaMigrator = migrator.getIfAvailable();
        if (schemaMigrator == null) {
            throw new IllegalStateException("Copying a schema requires a TenantSchemaMigrator to create the target structure");
        }

        log.info("Copying tenant schema {} to {}", sourceSchemaName, targetSchemaName);
        schemaMigrator.migrate(targetSchemaName);
        copyData(sourceSchemaName, targetSchemaName);
        jdbcTemplate.execute(UPDATE_SEQUENCES.formatted(targetSchemaName));

        eventPublisher.publishEvent(new TenantSchemaCopiedEvent(sourceSchemaName, targetSchemaName));
    }

    /**
     * Drops the schema with all its objects. {@code public}, the default schema and
     * {@code multitenancy.excluded-schemas} cannot be dropped.
     */
    public void dropSchema(String schemaName) {
        if (schemaName == null || isProtected(schemaName)) {
            throw new IllegalArgumentException("Schema " + schemaName + " is protected and cannot be dropped");
        }
        if (!schemaExists(schemaName)) {
            throw new IllegalArgumentException("Schema not found: " + schemaName);
        }

        log.info("Dropping tenant schema: {}", schemaName);
        jdbcTemplate.execute("DROP SCHEMA " + SchemaNames.quote(schemaName) + " CASCADE");

        eventPublisher.publishEvent(new TenantSchemaDroppedEvent(schemaName));
    }

    /**
     * Schemas in the database that belong to no tenant of the {@link TenantRegistry}, leaving out system schemas,
     * {@code public}, the default schema and {@code multitenancy.excluded-schemas}.
     */
    public List<String> findOrphanSchemas() {
        var registry = tenantRegistry.getIfAvailable();
        if (registry == null) {
            throw new IllegalStateException("Finding orphan schemas requires a TenantRegistry bean");
        }
        var tenantSchemas = new HashSet<>(registry.getTenantSchemas());
        return jdbcTemplate.queryForList(SELECT_SCHEMAS, String.class).stream()
                .filter(schema -> !isProtected(schema))
                .filter(schema -> !tenantSchemas.contains(schema))
                .toList();
    }

    private boolean isProtected(String schemaName) {
        return "public".equals(schemaName)
                || schemaName.equals(properties.getDefaultSchema())
                || properties.getExcludedSchemas().contains(schemaName);
    }

    private void requireNotExists(String schemaName) {
        if (schemaExists(schemaName)) {
            throw new IllegalArgumentException("Schema already exists: " + schemaName);
        }
    }

    private void copyData(String source, String target) {
        var tables = new ArrayList<>(jdbcTemplate.queryForList(SELECT_TABLES, String.class, source, properties.getMigration().getTable()));
        tables.sort(Comparator.comparingInt(this::getTablePriority).thenComparing(Comparator.naturalOrder()));

        for (var table : tables) {
            var sourceTable = SchemaNames.quote(source) + "." + SchemaNames.quote(table);
            var targetTable = SchemaNames.quote(target) + "." + SchemaNames.quote(table);
            log.debug("Copying data from {} to {}", sourceTable, targetTable);
            jdbcTemplate.execute("INSERT INTO " + targetTable + " SELECT * FROM " + sourceTable);
        }
    }

    private int getTablePriority(String tableName) {
        return priorityProviders.orderedStream()
                .map(provider -> provider.getTablePriority(tableName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(TableCopyPriorityProvider.DEFAULT_PRIORITY);
    }
}
