package org.dreamabout.sw.multitenancy.hibernate;

import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiSchemaConnectionProviderTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final MultitenancyProperties properties = new MultitenancyProperties();
    private final MultiSchemaConnectionProvider provider = new MultiSchemaConnectionProvider(dataSource, properties);

    @BeforeEach
    void setUp() throws SQLException {
        properties.setDefaultSchema("app_public");
        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void shouldSetTenantSchemaOnConnection() throws SQLException {
        var result = provider.getConnection(TenantIdentifier.of("tenant_a"));

        assertThat(result).isSameAs(connection);
        verify(connection).setSchema("tenant_a");
    }

    @Test
    void shouldUseDefaultSchemaForAnyConnection() throws SQLException {
        provider.getAnyConnection();

        verify(connection).setSchema("app_public");
    }

    @Test
    void shouldRejectNullTenant() {
        assertThatThrownBy(() -> provider.getConnection(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCloseConnectionWhenSchemaCannotBeSet() throws SQLException {
        var tenant = TenantIdentifier.of("tenant_a");
        doThrow(new SQLException("no schema")).when(connection).setSchema("tenant_a");

        assertThatThrownBy(() -> provider.getConnection(tenant))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tenant_a");
        verify(connection).close();
    }

    @Test
    void shouldResetSchemaAndCloseOnRelease() throws SQLException {
        provider.releaseConnection(TenantIdentifier.of("tenant_a"), connection);

        var order = inOrder(connection);
        order.verify(connection).setSchema("app_public");
        order.verify(connection).close();
    }

    @Test
    void shouldCloseAnyConnectionOnRelease() throws SQLException {
        provider.releaseAnyConnection(connection);

        verify(connection).close();
    }

    @Test
    void shouldNotSupportAggressiveRelease() {
        assertThat(provider.supportsAggressiveRelease()).isFalse();
    }

    @Test
    void shouldUnwrapAsItselfAndAsConnectionProvider() {
        assertThat(provider.isUnwrappableAs(MultiSchemaConnectionProvider.class)).isTrue();
        assertThat(provider.isUnwrappableAs(MultiTenantConnectionProvider.class)).isTrue();
        assertThat(provider.unwrap(MultiSchemaConnectionProvider.class)).isSameAs(provider);
        assertThat(provider.unwrap(MultiTenantConnectionProvider.class)).isSameAs(provider);
    }

    @Test
    void shouldRefuseToUnwrapUnrelatedType() {
        assertThat(provider.isUnwrappableAs(String.class)).isFalse();
        assertThatThrownBy(() -> provider.unwrap(String.class)).isInstanceOf(UnknownUnwrapTypeException.class);
    }

    @Test
    void shouldRegisterItselfInHibernateProperties() {
        Map<String, Object> hibernateProperties = new HashMap<>();

        provider.customize(hibernateProperties);

        assertThat(hibernateProperties).containsEntry(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
    }
}
