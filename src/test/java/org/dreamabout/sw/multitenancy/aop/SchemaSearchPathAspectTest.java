package org.dreamabout.sw.multitenancy.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaSearchPathAspectTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final SchemaSearchPathAspect aspect = new SchemaSearchPathAspect(jdbcTemplate, new MultitenancyProperties());

    @BeforeEach
    void setUp() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldUseDefaultSchemaWithoutTenant() throws Throwable {
        var result = aspect.aroundMultitenant(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(jdbcTemplate).execute("SET search_path TO \"public\"");
    }

    @Test
    void shouldPutQuotedTenantSchemaBeforeDefaultSchema() throws Throwable {
        TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));

        aspect.aroundMultitenant(joinPoint);

        verify(jdbcTemplate).execute("SET search_path TO \"tenant_a\", \"public\"");
    }

    @Test
    void shouldQuoteHostileTenantIdentifier() throws Throwable {
        TenantContext.setCurrentTenant(TenantIdentifier.of("x\"; DROP SCHEMA public; --"));

        aspect.aroundMultitenant(joinPoint);

        verify(jdbcTemplate).execute("SET search_path TO \"x\"\"; DROP SCHEMA public; --\", \"public\"");
    }

    @Test
    void shouldNotResetUnchangedSearchPath() throws Throwable {
        TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));

        aspect.aroundMultitenant(joinPoint);
        aspect.aroundMultitenant(joinPoint);

        verify(jdbcTemplate, times(1)).execute(anyString());
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void shouldProceedWithoutSqlWhenPathAlreadyMatches() throws Throwable {
        MultitenancyThreadContext.setCurrentSearchPath("\"public\"");

        aspect.aroundMultitenant(joinPoint);

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
