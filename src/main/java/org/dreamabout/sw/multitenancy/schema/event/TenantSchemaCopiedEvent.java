package org.dreamabout.sw.multitenancy.schema.event;

/**
 * Published after a tenant schema was copied (structure and data) into a new schema.
 */
public record TenantSchemaCopiedEvent(String sourceSchemaName, String targetSchemaName) {
}
