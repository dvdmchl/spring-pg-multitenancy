package org.dreamabout.sw.multitenancy.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultitenancyTaskDecoratorTest {

    private final MultitenancyTaskDecorator decorator = new MultitenancyTaskDecorator();

    @AfterEach
    void tearDown() {
        MultitenancyThreadContext.clear();
    }

    @Test
    void shouldRunTaskAndClearContextAfterwards() {
        var seenTenant = new AtomicReference<TenantIdentifier>();
        var task = decorator.decorate(() -> {
            TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));
            seenTenant.set(TenantContext.getCurrentTenant());
        });

        task.run();

        assertThat(seenTenant.get().getTenantId()).isEqualTo("tenant_a");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void shouldClearContextWhenTaskFails() {
        var task = decorator.decorate(() -> {
            TenantContext.setCurrentTenant(TenantIdentifier.of("tenant_a"));
            throw new IllegalStateException("boom");
        });

        assertThatThrownBy(task::run).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
