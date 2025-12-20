package org.dreamabout.sw.multitenancy.config;

import org.dreamabout.sw.multitenancy.aop.SchemaSearchPathAspect;
import org.dreamabout.sw.multitenancy.hibernate.MultiSchemaConnectionProvider;
import org.dreamabout.sw.multitenancy.hibernate.TenantIdentifierResolver;
import org.dreamabout.sw.multitenancy.web.MultitenancyContextFilter;
import org.dreamabout.sw.multitenancy.web.TenantInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({SchemaSearchPathAspect.class, MultiSchemaConnectionProvider.class, TenantIdentifierResolver.class, MultitenancyContextFilter.class, TenantInterceptor.class})
@EnableConfigurationProperties(MultitenancyProperties.class)
public class MultitenancyAutoConfiguration {
}
