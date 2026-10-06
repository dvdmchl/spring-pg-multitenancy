package org.dreamabout.sw.multitenancy.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnCurrentTenantWhenSet() {
        TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));

        assertThat(TenantContext.getCurrentTenant().getTenantId()).isEqualTo("tenant_a");
    }

    @Test
    void shouldReturnNullAfterClear() {
        TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));

        TenantContext.clear();

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
