package org.dreamabout.sw.multitenancy.core;

public interface TenantIdentifier {
    String getTenantId();

    static TenantIdentifier of(String tenantId) {
        return () -> tenantId;
    }
}
