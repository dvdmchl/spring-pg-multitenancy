package org.dreamabout.sw.multitenancy.schema;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Validation and quoting of PostgreSQL schema names.
 */
@UtilityClass
public class SchemaNames {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    /**
     * Whether the name is a valid tenant schema name: starts with a lowercase letter, contains only
     * lowercase letters, digits and underscores, at most 63 characters.
     */
    public static boolean isValid(String schemaName) {
        return schemaName != null && VALID_NAME.matcher(schemaName).matches();
    }

    public static void validate(String schemaName) {
        if (!isValid(schemaName)) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName
                    + ". Must start with a letter and contain only lowercase letters, numbers, and underscores (max 63 characters).");
        }
    }

    /**
     * Quotes the name as a PostgreSQL identifier.
     */
    public static String quote(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
