package org.dreamabout.sw.multitenancy.schema;

/**
 * Creates a tenant schema if needed and brings its structure up to date.
 */
@FunctionalInterface
public interface TenantSchemaMigrator {

    void migrate(String schemaName);
}
