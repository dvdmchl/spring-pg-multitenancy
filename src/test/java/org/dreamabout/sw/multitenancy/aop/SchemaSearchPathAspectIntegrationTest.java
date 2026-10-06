package org.dreamabout.sw.multitenancy.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class SchemaSearchPathAspectIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.1");

    private final SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), true);
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        dataSource.destroy();
    }

    @Test
    void shouldResolveTablesFromTenantSchemaFirst() throws Throwable {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS tenant_a");
        TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));
        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenAnswer(invocation ->
                jdbcTemplate.queryForList("SELECT unnest(current_schemas(false))", String.class));

        var schemas = new SchemaSearchPathAspect(jdbcTemplate, new MultitenancyProperties()).aroundMultitenant(joinPoint);

        assertThat(schemas).isEqualTo(List.of("tenant_a", "public"));
    }
}
