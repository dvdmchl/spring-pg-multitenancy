package org.dreamabout.sw.multitenancy.web;

import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TenantInterceptorTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetResolvedTenantAndContinue() {
        var tenant = TenantIdentifier.of("tenant_a");
        var interceptor = new TenantInterceptor(() -> tenant);

        var proceed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContext.getCurrentTenant()).isSameAs(tenant);
    }
}
