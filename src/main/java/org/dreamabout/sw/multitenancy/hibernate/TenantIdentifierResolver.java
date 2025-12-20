package org.dreamabout.sw.multitenancy.hibernate;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.dreamabout.sw.multitenancy.core.TenantResolver;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<TenantIdentifier>, HibernatePropertiesCustomizer {

    private final TenantResolver tenantResolver;

    @Override
    public TenantIdentifier resolveCurrentTenantIdentifier() {
        var currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            return currentTenant;
        }
        return tenantResolver.getCurrentTenantIdentifier();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }

}
