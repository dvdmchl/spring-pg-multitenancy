package org.dreamabout.sw.multitenancy.schema;

import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCopiedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCreatedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaDroppedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TenantSchemaManagerIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.1");

    private final List<Object> events = new ArrayList<>();
    private final Set<String> tenants = new java.util.HashSet<>();
    private JdbcTemplate jdbcTemplate;
    private TenantSchemaManager manager;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                DO $$
                DECLARE s text;
                BEGIN
                    FOR s IN SELECT nspname FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname NOT IN ('information_schema', 'public')
                    LOOP EXECUTE format('DROP SCHEMA %I CASCADE', s); END LOOP;
                END $$;
                """);

        var properties = new MultitenancyProperties();
        properties.setExcludedSchemas(List.of("template"));
        TenantRegistry registry = () -> tenants;
        var beanFactory = new StaticListableBeanFactory(Map.of(
                "migrator", new FlywayTenantSchemaMigrator(dataSource, properties),
                "registry", registry,
                "priorities", (TableCopyPriorityProvider) table -> table.equals("parent") ? 1 : null));
        manager = new TenantSchemaManager(jdbcTemplate, properties, events::add,
                beanFactory.getBeanProvider(TenantSchemaMigrator.class),
                beanFactory.getBeanProvider(TenantRegistry.class),
                beanFactory.getBeanProvider(TableCopyPriorityProvider.class));
    }

    @Test
    void createCopyAndDropSchema() {
        manager.createSchema("tenant_a");
        assertThat(manager.schemaExists("tenant_a")).isTrue();
        assertThat(events).containsExactly(new TenantSchemaCreatedEvent("tenant_a"));

        jdbcTemplate.update("INSERT INTO tenant_a.parent (name) VALUES ('p1'), ('p2')");
        jdbcTemplate.update("INSERT INTO tenant_a.child (parent_id) VALUES (2)");

        manager.copySchema("tenant_a", "tenant_b");

        assertThat(jdbcTemplate.queryForList("SELECT name FROM tenant_b.parent ORDER BY id", String.class))
                .containsExactly("p1", "p2");
        assertThat(jdbcTemplate.queryForObject("SELECT parent_id FROM tenant_b.child", Long.class)).isEqualTo(2L);
        var nextId = jdbcTemplate.queryForObject("INSERT INTO tenant_b.parent (name) VALUES ('p3') RETURNING id", Long.class);
        assertThat(nextId).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_b.flyway_schema_history WHERE version IS NOT NULL", Integer.class))
                .isEqualTo(1);

        manager.dropSchema("tenant_b");
        assertThat(manager.schemaExists("tenant_b")).isFalse();
        assertThat(events).containsExactly(
                new TenantSchemaCreatedEvent("tenant_a"),
                new TenantSchemaCopiedEvent("tenant_a", "tenant_b"),
                new TenantSchemaDroppedEvent("tenant_b"));
    }

    @Test
    void findOrphanSchemas() {
        manager.createSchema("tenant_a");
        manager.createSchema("orphan");
        manager.createSchema("template");
        tenants.add("tenant_a");

        assertThat(manager.findOrphanSchemas()).containsExactly("orphan");
    }

    @Test
    void migrationIsIdempotent() {
        manager.createSchema("tenant_a");
        tenants.add("tenant_a");

        new TenantMigrationRunner(() -> tenants, new FlywayTenantSchemaMigrator(
                jdbcTemplate.getDataSource(), new MultitenancyProperties())).run(null);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_a.flyway_schema_history WHERE version IS NOT NULL", Integer.class))
                .isEqualTo(1);
    }
}
