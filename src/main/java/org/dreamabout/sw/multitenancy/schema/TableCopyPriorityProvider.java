package org.dreamabout.sw.multitenancy.schema;

/**
 * Decides the order in which tables are copied by {@link TenantSchemaManager#copySchema(String, String)},
 * so that referenced tables are filled before the tables referencing them.
 */
@FunctionalInterface
public interface TableCopyPriorityProvider {

    /**
     * Tables without a priority from any provider.
     */
    int DEFAULT_PRIORITY = 10;

    /**
     * Returns the priority for the given table. Lower value means higher priority (copied earlier).
     * Returns null if this provider doesn't handle the table.
     */
    Integer getTablePriority(String tableName);
}
