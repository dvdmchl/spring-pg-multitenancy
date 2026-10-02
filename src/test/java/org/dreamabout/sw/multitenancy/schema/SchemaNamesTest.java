package org.dreamabout.sw.multitenancy.schema;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaNamesTest {

    @Test
    void validNames() {
        assertThat(SchemaNames.isValid("tenant_1")).isTrue();
        assertThat(SchemaNames.isValid("a".repeat(63))).isTrue();
    }

    @Test
    void invalidNames() {
        assertThat(SchemaNames.isValid(null)).isFalse();
        assertThat(SchemaNames.isValid("")).isFalse();
        assertThat(SchemaNames.isValid("1tenant")).isFalse();
        assertThat(SchemaNames.isValid("Tenant")).isFalse();
        assertThat(SchemaNames.isValid("tenant; DROP SCHEMA public")).isFalse();
        assertThat(SchemaNames.isValid("a".repeat(64))).isFalse();
        assertThatThrownBy(() -> SchemaNames.validate("x\"y")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quoteEscapesDoubleQuotes() {
        assertThat(SchemaNames.quote("tenant")).isEqualTo("\"tenant\"");
        assertThat(SchemaNames.quote("a\"b")).isEqualTo("\"a\"\"b\"");
    }
}
