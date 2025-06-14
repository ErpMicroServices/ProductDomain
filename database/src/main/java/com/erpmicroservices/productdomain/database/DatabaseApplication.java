package com.erpmicroservices.productdomain.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot Application for ProductDomain Database module.
 * 
 * This application provides database configuration, schema management,
 * and data access capabilities for the ProductDomain microservice.
 */
@SpringBootApplication
@EnableTransactionManagement
public class DatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseApplication.class, args);
    }
}