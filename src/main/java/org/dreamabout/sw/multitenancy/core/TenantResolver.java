package org.dreamabout.sw.multitenancy.core;

public interface TenantResolver {
    TenantIdentifier getCurrentTenantIdentifier();
}
