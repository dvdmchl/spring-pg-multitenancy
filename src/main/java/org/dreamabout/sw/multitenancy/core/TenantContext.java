package org.dreamabout.sw.multitenancy.core;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantContext {

    public static void setCurrentTenant(TenantIdentifier tenantIdentifier) {
        MultitenancyThreadContext.setTenantIdentifier(tenantIdentifier);
    }

    public static TenantIdentifier getCurrentTenant() {
        return MultitenancyThreadContext.getTenantIdentifier();
    }

    public static void clear() {
        MultitenancyThreadContext.clear();
    }

}
