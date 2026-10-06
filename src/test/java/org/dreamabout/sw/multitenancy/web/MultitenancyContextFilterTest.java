package org.dreamabout.sw.multitenancy.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultitenancyContextFilterTest {

    private final MultitenancyContextFilter filter = new MultitenancyContextFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldClearContextAfterRequest() throws Exception {
        FilterChain chain = (req, res) -> TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));

        filter.doFilter(request, response, chain);

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void shouldClearContextWhenChainFails() {
        FilterChain chain = (req, res) -> {
            TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(ServletException.class);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
