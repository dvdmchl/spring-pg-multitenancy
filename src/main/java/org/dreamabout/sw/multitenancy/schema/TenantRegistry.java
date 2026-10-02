package org.dreamabout.sw.multitenancy.schema;

import java.util.Collection;

/**
 * Supplied by the application: the schemas of all known tenants.
 * <p>
 * Used to migrate all tenant schemas on startup and to find schemas that no tenant owns.
 */
@FunctionalInterface
public interface TenantRegistry {

    Collection<String> getTenantSchemas();
}
