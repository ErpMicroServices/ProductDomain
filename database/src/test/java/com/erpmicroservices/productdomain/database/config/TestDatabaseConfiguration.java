package com.erpmicroservices.productdomain.database.config;

import com.erpmicroservices.productdomain.database.TestDatabaseApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test configuration for Cucumber database tests.
 * 
 * This configuration ensures that tests use H2 by default via the 'test' profile,
 * unless overridden by environment variables (e.g., for PostgreSQL compatibility matrix tests).
 */
@CucumberContextConfiguration
@SpringBootTest(classes = TestDatabaseApplication.class)
@ActiveProfiles("test")
public class TestDatabaseConfiguration {
    // This class serves as the configuration entry point for Cucumber tests
    // The @ActiveProfiles("test") annotation ensures H2 is used by default
}