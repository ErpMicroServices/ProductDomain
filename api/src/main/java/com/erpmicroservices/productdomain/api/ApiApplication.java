package com.erpmicroservices.productdomain.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot Application class for ProductDomain API.
 * 
 * This application provides a GraphQL API for product management
 * with OAuth2 security, monitoring, and comprehensive product operations.
 */
@SpringBootApplication(scanBasePackages = {
    "com.erpmicroservices.productdomain.api",
    "com.erpmicroservices.productdomain.database"
})
@EnableJpaRepositories(basePackages = "com.erpmicroservices.productdomain.database.repository")
@EntityScan(basePackages = "com.erpmicroservices.productdomain.database.entity")
@EnableJpaAuditing
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}