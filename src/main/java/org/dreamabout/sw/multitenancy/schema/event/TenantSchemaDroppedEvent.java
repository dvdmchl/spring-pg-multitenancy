package org.dreamabout.sw.multitenancy.schema.event;

/**
 * Published after a tenant schema was dropped.
 */
public record TenantSchemaDroppedEvent(String schemaName) {
}
