package org.dreamabout.sw.multitenancy.schema.event;

/**
 * Published after a tenant schema was created and migrated.
 */
public record TenantSchemaCreatedEvent(String schemaName) {
}
