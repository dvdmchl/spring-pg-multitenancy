package org.dreamabout.sw.multitenancy.config;

import org.dreamabout.sw.multitenancy.schema.FlywayTenantSchemaMigrator;
import org.dreamabout.sw.multitenancy.schema.TenantMigrationRunner;
import org.dreamabout.sw.multitenancy.schema.TenantRegistry;
import org.dreamabout.sw.multitenancy.schema.TenantSchemaManager;
import org.dreamabout.sw.multitenancy.schema.TenantSchemaMigrator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TenantSchemaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantSchemaAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void registersManagerAndFlywayMigrator() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TenantSchemaManager.class);
            assertThat(context).hasSingleBean(FlywayTenantSchemaMigrator.class);
            assertThat(context).doesNotHaveBean(TenantMigrationRunner.class);
        });
    }

    @Test
    void noMigratorWithoutFlyway() {
        runner.withClassLoader(new FilteredClassLoader("org.flywaydb.core"))
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantSchemaManager.class);
                    assertThat(context).doesNotHaveBean(TenantSchemaMigrator.class);
                });
    }

    @Test
    void customMigratorReplacesFlyway() {
        runner.withBean(TenantSchemaMigrator.class, () -> schema -> { })
                .run(context -> assertThat(context).hasSingleBean(TenantSchemaMigrator.class)
                        .doesNotHaveBean(FlywayTenantSchemaMigrator.class));
    }

    @Test
    void migrationRunnerWithRegistry() {
        runner.withBean(TenantRegistry.class, () -> () -> List.of("tenant_a"))
                .run(context -> assertThat(context).hasSingleBean(TenantMigrationRunner.class));
    }

    @Test
    void migrationRunnerCanBeDisabled() {
        runner.withBean(TenantRegistry.class, () -> () -> List.of("tenant_a"))
                .withPropertyValues("multitenancy.migration.run-on-startup=false")
                .run(context -> assertThat(context).doesNotHaveBean(TenantMigrationRunner.class));
    }

    @Test
    void bindsMigrationProperties() {
        runner.withPropertyValues(
                        "multitenancy.excluded-schemas=template",
                        "multitenancy.migration.locations=classpath:db/tenant",
                        "multitenancy.migration.clean-before-migrate=true")
                .run(context -> {
                    var properties = context.getBean(MultitenancyProperties.class);
                    assertThat(properties.getExcludedSchemas()).containsExactly("template");
                    assertThat(properties.getMigration().getLocations()).containsExactly("classpath:db/tenant");
                    assertThat(properties.getMigration().isCleanBeforeMigrate()).isTrue();
                });
    }
}
