package org.dreamabout.sw.multitenancy.hibernate;

import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.hibernate.cfg.MultiTenancySettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdentifierResolverTest {

    private final TenantIdentifier resolvedTenant = TenantIdentifier.of("resolved");
    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver(() -> resolvedTenant);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPreferTenantFromContext() {
        var contextTenant = TenantIdentifier.of("context");
        TenantContext.setCurrentTenant(contextTenant);

        assertThat(resolver.resolveCurrentTenantIdentifier()).isSameAs(contextTenant);
    }

    @Test
    void shouldFallBackToTenantResolverWhenContextIsEmpty() {
        assertThat(resolver.resolveCurrentTenantIdentifier()).isSameAs(resolvedTenant);
    }

    @Test
    void shouldValidateExistingSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
    }

    @Test
    void shouldRegisterItselfInHibernateProperties() {
        Map<String, Object> hibernateProperties = new HashMap<>();

        resolver.customize(hibernateProperties);

        assertThat(hibernateProperties).containsEntry(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
