package org.dreamabout.sw.multitenancy.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class MultitenancyThreadContextTest {

    @AfterEach
    void tearDown() {
        MultitenancyThreadContext.clear();
    }

    @Test
    void shouldReturnNullWhenNothingWasSet() {
        assertThat(MultitenancyThreadContext.<String>get("missing")).isNull();
        assertThat(MultitenancyThreadContext.getTenantIdentifier()).isNull();
        assertThat(MultitenancyThreadContext.getCurrentSearchPath()).isNull();
    }

    @Test
    void shouldReturnStoredValues() {
        var tenant = TenantIdentifier.of("tenant_a");

        MultitenancyThreadContext.set("key", 42);
        MultitenancyThreadContext.setTenantIdentifier(tenant);
        MultitenancyThreadContext.setCurrentSearchPath("tenant_a, public");

        assertThat(MultitenancyThreadContext.<Integer>get("key")).isEqualTo(42);
        assertThat(MultitenancyThreadContext.getTenantIdentifier()).isSameAs(tenant);
        assertThat(MultitenancyThreadContext.getCurrentSearchPath()).isEqualTo("tenant_a, public");
    }

    @Test
    void shouldForgetValuesWhenCleared() {
        MultitenancyThreadContext.set("key", 42);

        MultitenancyThreadContext.clear();

        assertThat(MultitenancyThreadContext.<Integer>get("key")).isNull();
    }

    @Test
    void shouldKeepValuesPerThread() {
        MultitenancyThreadContext.set("key", "main");

        var otherThreadValue = CompletableFuture.supplyAsync(() -> MultitenancyThreadContext.<String>get("key")).join();

        assertThat(otherThreadValue).isNull();
        assertThat(MultitenancyThreadContext.<String>get("key")).isEqualTo("main");
    }
}
