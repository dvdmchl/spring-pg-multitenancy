package org.dreamabout.sw.multitenancy.config;

import org.dreamabout.sw.multitenancy.schema.FlywayTenantSchemaMigrator;
import org.dreamabout.sw.multitenancy.schema.TableCopyPriorityProvider;
import org.dreamabout.sw.multitenancy.schema.TenantMigrationRunner;
import org.dreamabout.sw.multitenancy.schema.TenantRegistry;
import org.dreamabout.sw.multitenancy.schema.TenantSchemaManager;
import org.dreamabout.sw.multitenancy.schema.TenantSchemaMigrator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Tenant schema lifecycle: per-tenant migrations, schema operations and the startup migration of all tenants.
 */
@AutoConfiguration(after = MultitenancyAutoConfiguration.class)
@EnableConfigurationProperties(MultitenancyProperties.class)
public class TenantSchemaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantSchemaManager tenantSchemaManager(JdbcTemplate jdbcTemplate,
                                                  MultitenancyProperties properties,
                                                  ApplicationEventPublisher eventPublisher,
                                                  ObjectProvider<TenantSchemaMigrator> migrator,
                                                  ObjectProvider<TenantRegistry> tenantRegistry,
                                                  ObjectProvider<TableCopyPriorityProvider> priorityProviders) {
        return new TenantSchemaManager(jdbcTemplate, properties, eventPublisher, migrator, tenantRegistry, priorityProviders);
    }

    @Bean
    @ConditionalOnBean({TenantRegistry.class, TenantSchemaMigrator.class})
    @ConditionalOnProperty(prefix = "multitenancy.migration", name = "run-on-startup", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TenantMigrationRunner tenantMigrationRunner(TenantRegistry tenantRegistry, TenantSchemaMigrator migrator) {
        return new TenantMigrationRunner(tenantRegistry, migrator);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.flywaydb.core.Flyway")
    static class FlywayMigratorConfiguration {

        @Bean
        @ConditionalOnMissingBean(TenantSchemaMigrator.class)
        public FlywayTenantSchemaMigrator flywayTenantSchemaMigrator(DataSource dataSource, MultitenancyProperties properties) {
            return new FlywayTenantSchemaMigrator(dataSource, properties);
        }
    }
}
