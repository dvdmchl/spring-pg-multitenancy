package org.dreamabout.sw.multitenancy.schema;

import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class FlywayTenantSchemaMigratorIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.1");

    private static final String SCHEMA = "tenant_a";

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private MultitenancyProperties properties;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        properties = new MultitenancyProperties();
        migrator().migrate(SCHEMA);
    }

    @Test
    void shouldCleanSchemaBeforeMigrateWhenEnabled() {
        jdbcTemplate.update("INSERT INTO tenant_a.parent (name) VALUES ('p1')");
        properties.getMigration().setCleanBeforeMigrate(true);

        migrator().migrate(SCHEMA);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_a.parent", Integer.class)).isZero();
    }

    @Test
    void shouldRepairAndMigrateWhenValidationFails() {
        corruptChecksum();

        migrator().migrate(SCHEMA);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT checksum FROM tenant_a.flyway_schema_history WHERE version = '1'", Integer.class))
                .isNotEqualTo(1);
    }

    @Test
    void shouldFailOnValidationErrorWhenRepairIsDisabled() {
        corruptChecksum();
        properties.getMigration().setRepairOnValidationError(false);
        var migrator = migrator();

        assertThatThrownBy(() -> migrator.migrate(SCHEMA)).isInstanceOf(FlywayValidateException.class);
    }

    private FlywayTenantSchemaMigrator migrator() {
        return new FlywayTenantSchemaMigrator(dataSource, properties);
    }

    private void corruptChecksum() {
        jdbcTemplate.update("UPDATE tenant_a.flyway_schema_history SET checksum = 1 WHERE version = '1'");
    }
}
