package com.example.bankcards;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared setup for the MockMvc integration tests. Each test method runs in a
 * transaction that is rolled back afterwards, which isolates tests from each
 * other without rebuilding the Spring context between them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {
}
