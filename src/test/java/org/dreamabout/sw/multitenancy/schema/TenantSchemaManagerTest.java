package org.dreamabout.sw.multitenancy.schema;

import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCopiedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCreatedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaDroppedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TenantSchemaManagerTest {

    private JdbcTemplate jdbcTemplate;
    private ApplicationEventPublisher eventPublisher;
    private TenantSchemaMigrator migrator;
    private MultitenancyProperties properties;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        migrator = mock(TenantSchemaMigrator.class);
        properties = new MultitenancyProperties();
        properties.setDefaultSchema("app_public");
        properties.setExcludedSchemas(List.of("template"));
    }

    private TenantSchemaManager manager(Map<String, Object> beans) {
        var beanFactory = new StaticListableBeanFactory(beans);
        return new TenantSchemaManager(jdbcTemplate, properties, eventPublisher,
                beanFactory.getBeanProvider(TenantSchemaMigrator.class),
                beanFactory.getBeanProvider(TenantRegistry.class),
                beanFactory.getBeanProvider(TableCopyPriorityProvider.class));
    }

    private void schemaExists(String schemaName, boolean exists) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(schemaName))).thenReturn(exists);
    }

    @Test
    void createSchemaRunsMigrationsAndPublishesEvent() {
        schemaExists("tenant_a", false);

        manager(Map.of("migrator", migrator)).createSchema("tenant_a");

        verify(migrator).migrate("tenant_a");
        verify(eventPublisher).publishEvent(new TenantSchemaCreatedEvent("tenant_a"));
    }

    @Test
    void createSchemaWithoutMigratorCreatesEmptySchema() {
        schemaExists("tenant_a", false);

        manager(Map.of()).createSchema("tenant_a");

        verify(jdbcTemplate).execute("CREATE SCHEMA \"tenant_a\"");
        verify(eventPublisher).publishEvent(new TenantSchemaCreatedEvent("tenant_a"));
    }

    @Test
    void createSchemaRejectsInvalidAndExistingNames() {
        var manager = manager(Map.of("migrator", migrator));
        schemaExists("tenant_a", true);

        assertThatThrownBy(() -> manager.createSchema("bad name")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.createSchema("tenant_a")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(migrator, eventPublisher);
    }

    @Test
    void copySchemaCopiesTablesInPriorityOrderAndResetsSequences() {
        schemaExists("source", true);
        schemaExists("target", false);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("source"), eq("flyway_schema_history")))
                .thenReturn(new ArrayList<>(List.of("b_child", "a_other", "z_parent")));
        TableCopyPriorityProvider priorities = table -> table.equals("z_parent") ? 1 : null;

        manager(Map.of("migrator", migrator, "priorities", priorities)).copySchema("source", "target");

        InOrder order = inOrder(migrator, jdbcTemplate, eventPublisher);
        order.verify(migrator).migrate("target");
        order.verify(jdbcTemplate).execute("INSERT INTO \"target\".\"z_parent\" SELECT * FROM \"source\".\"z_parent\"");
        order.verify(jdbcTemplate).execute("INSERT INTO \"target\".\"a_other\" SELECT * FROM \"source\".\"a_other\"");
        order.verify(jdbcTemplate).execute("INSERT INTO \"target\".\"b_child\" SELECT * FROM \"source\".\"b_child\"");
        var sequences = ArgumentCaptor.forClass(String.class);
        order.verify(jdbcTemplate).execute(sequences.capture());
        assertThat(sequences.getValue()).contains("setval").contains("s.nspname = 'target'");
        order.verify(eventPublisher).publishEvent(new TenantSchemaCopiedEvent("source", "target"));
    }

    @Test
    void copySchemaRequiresExistingSourceAndMigrator() {
        schemaExists("missing", false);
        schemaExists("source", true);
        schemaExists("target", false);

        assertThatThrownBy(() -> manager(Map.of("migrator", migrator)).copySchema("missing", "target"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager(Map.of()).copySchema("source", "target"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(migrator, eventPublisher);
    }

    @Test
    void dropSchemaQuotesNameAndPublishesEvent() {
        schemaExists("odd\"name", true);

        manager(Map.of()).dropSchema("odd\"name");

        verify(jdbcTemplate).execute("DROP SCHEMA \"odd\"\"name\" CASCADE");
        verify(eventPublisher).publishEvent(new TenantSchemaDroppedEvent("odd\"name"));
    }

    @Test
    void dropSchemaRefusesProtectedSchemas() {
        var manager = manager(Map.of());

        for (var schema : List.of("public", "app_public", "template")) {
            assertThatThrownBy(() -> manager.dropSchema(schema)).isInstanceOf(IllegalArgumentException.class);
        }
        verify(jdbcTemplate, never()).execute(anyString());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void findOrphanSchemasLeavesOutTenantsAndProtectedSchemas() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("app_public", "orphan", "public", "template", "tenant_a"));
        TenantRegistry registry = () -> List.of("tenant_a");

        assertThat(manager(Map.of("registry", registry)).findOrphanSchemas()).containsExactly("orphan");
    }

    @Test
    void findOrphanSchemasRequiresRegistry() {
        assertThatThrownBy(() -> manager(Map.of()).findOrphanSchemas()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void migrationRunnerContinuesAfterFailure() throws Exception {
        TenantRegistry registry = () -> List.of("tenant_a", "tenant_b");
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(migrator).migrate("tenant_a");

        new TenantMigrationRunner(registry, migrator).run(null);

        verify(migrator, times(1)).migrate("tenant_a");
        verify(migrator, times(1)).migrate("tenant_b");
    }
}
