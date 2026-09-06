package com.example.bankcards;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for the tests that must run against a real PostgreSQL rather than H2.
 *
 * <p>The container is a singleton started once per JVM and shared by every
 * subclass, so the whole PostgreSQL layer costs one container and one Spring
 * context. Testcontainers' Ryuk sidecar removes it when the JVM exits; nothing
 * touches a locally installed PostgreSQL, and the port is allocated
 * dynamically rather than fixed.
 *
 * <p>Requires a running Docker daemon. The image matches the one in
 * docker-compose.yml so tests and production run the same PostgreSQL version.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres-test")
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }
}
