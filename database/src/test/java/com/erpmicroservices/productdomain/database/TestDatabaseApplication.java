package com.erpmicroservices.productdomain.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test-specific Spring Boot Application for ProductDomain Database module.
 * 
 * This application configuration is used exclusively for running tests
 * and ensures proper test context initialization.
 */
@SpringBootApplication
@EnableTransactionManagement
@Profile("test")
public class TestDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestDatabaseApplication.class, args);
    }
}