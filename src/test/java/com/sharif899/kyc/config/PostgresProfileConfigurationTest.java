package com.sharif899.kyc.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two datasource configurations without needing a live
 * database. The postgres profile is verified by reading its properties
 * file: the connection must come from environment variables, the
 * dialect and schema mode must be Postgres-appropriate, and the
 * default profile must still fall back to in-memory H2 so the service
 * runs with zero setup.
 *
 * A real integration test against a live PostgreSQL instance is
 * deliberately not in this suite — there is no database in CI, and
 * one would need a real connection string. Run the service with
 * --spring.profiles.active=postgres against Supabase instead (README).
 */
class PostgresProfileConfigurationTest {

    private Properties load(String resource) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource + " must exist on the classpath");
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    @Test
    void postgresProfileExistsAndIsFullyEnvironmentDriven() throws IOException {
        Properties postgres = load("application-postgres.properties");

        assertEquals("org.postgresql.Driver",
                postgres.getProperty("spring.datasource.driver-class-name"));
        // No host, username or password anywhere in the file: the
        // connection is assembled from environment variables only.
        assertEquals("${SPRING_DATASOURCE_URL}",
                postgres.getProperty("spring.datasource.url"));
        assertEquals("${SPRING_DATASOURCE_USERNAME}",
                postgres.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD}",
                postgres.getProperty("spring.datasource.password"));
        assertTrue(postgres.getProperty("spring.datasource.url").contains("SPRING_DATASOURCE_URL"));
    }

    @Test
    void postgresProfileUsesPostgresDialectAndUpdateSchemaMode() throws IOException {
        Properties postgres = load("application-postgres.properties");

        assertEquals("org.hibernate.dialect.PostgreSQLDialect",
                postgres.getProperty("spring.jpa.properties.hibernate.dialect"));
        // update creates the tables on first run and keeps them; the
        // destructive create-drop of the default profile must never
        // apply to a real database.
        assertEquals("update", postgres.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @Test
    void postgresProfileHasSensiblePoolSettings() throws IOException {
        Properties postgres = load("application-postgres.properties");

        int maxPoolSize = Integer.parseInt(
                postgres.getProperty("spring.datasource.hikari.maximum-pool-size"));
        int minIdle = Integer.parseInt(
                postgres.getProperty("spring.datasource.hikari.minimum-idle"));
        assertTrue(maxPoolSize > 0 && maxPoolSize <= 20,
                "pool must stay small — Supabase free tier limits connections");
        assertTrue(minIdle >= 0 && minIdle <= maxPoolSize);
        assertNotNull(postgres.getProperty("spring.datasource.hikari.connection-timeout"));
        assertNotNull(postgres.getProperty("spring.datasource.hikari.max-lifetime"));
    }

    @Test
    void defaultProfileStillFallsBackToInMemoryH2() throws IOException {
        Properties defaults = load("application.properties");

        assertTrue(defaults.getProperty("spring.datasource.url")
                        .startsWith("${SPRING_DATASOURCE_URL:jdbc:h2:mem:kycdb"),
                "without env vars the service must still start on in-memory H2");
        assertTrue(defaults.getProperty("spring.datasource.driver-class-name")
                .contains("org.h2.Driver"));
        assertEquals("create-drop", defaults.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @Test
    void noRealHostOrSecretInEitherPropertiesFile() throws IOException {
        for (String resource : new String[]{"application.properties", "application-postgres.properties"}) {
            Properties properties = load(resource);
            for (String value : properties.stringPropertyNames()) {
                String v = properties.getProperty(value);
                assertTrue(!v.contains("supabase.co") && !v.contains("amazonaws.com")
                                && !v.contains("postgres://") && !v.contains("jdbc:postgresql://"),
                        resource + ": " + value + " must not contain a real host");
            }
        }
    }
}
