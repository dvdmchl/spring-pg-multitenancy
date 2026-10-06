package org.dreamabout.sw.multitenancy.config;

import org.dreamabout.sw.multitenancy.aop.SchemaSearchPathAspect;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.dreamabout.sw.multitenancy.core.TenantResolver;
import org.dreamabout.sw.multitenancy.hibernate.MultiSchemaConnectionProvider;
import org.dreamabout.sw.multitenancy.hibernate.TenantIdentifierResolver;
import org.dreamabout.sw.multitenancy.web.MultitenancyContextFilter;
import org.dreamabout.sw.multitenancy.web.TenantInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MultitenancyAutoConfigurationTest {

    @Test
    void shouldRegisterMultitenancyBeansAndBindProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MultitenancyAutoConfiguration.class))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(TenantResolver.class, () -> () -> TenantIdentifier.of("tenant_a"))
                .withPropertyValues("multitenancy.default-schema=app_public")
                .run(context -> {
                    assertThat(context).hasSingleBean(SchemaSearchPathAspect.class)
                            .hasSingleBean(MultiSchemaConnectionProvider.class)
                            .hasSingleBean(TenantIdentifierResolver.class)
                            .hasSingleBean(MultitenancyContextFilter.class)
                            .hasSingleBean(TenantInterceptor.class);
                    assertThat(context.getBean(MultitenancyProperties.class).getDefaultSchema())
                            .isEqualTo("app_public");
                });
    }
}
