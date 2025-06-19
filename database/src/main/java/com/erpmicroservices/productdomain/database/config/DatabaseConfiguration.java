package com.erpmicroservices.productdomain.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database configuration for ProductDomain microservice.
 * 
 * Configures PostgreSQL datasource with HikariCP connection pooling.
 * Database schema is managed by JPA/Hibernate DDL auto-generation.
 */
@Configuration
public class DatabaseConfiguration {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String databaseDriver;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * Configure primary datasource with HikariCP connection pooling.
     * 
     * @return configured HikariDataSource
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(databaseDriver);
        
        // Connection pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // Performance and reliability settings
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("ProductDomainPool");
        config.setRegisterMbeans(true);
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }


    /**
     * Test-specific datasource configuration for integration tests.
     * Uses environment-specific database configuration.
     */
    @Configuration
    @Profile("test")
    static class TestDatabaseConfiguration {

        @Value("${spring.datasource.url}")
        private String databaseUrl;

        @Value("${spring.datasource.username}")
        private String databaseUsername;

        @Value("${spring.datasource.password}")
        private String databasePassword;

        @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
        private String databaseDriver;

        @Bean
        @Primary
        public DataSource testDataSource() {
            HikariConfig config = new HikariConfig();
            
            // Use configuration from properties/environment
            config.setJdbcUrl(databaseUrl);
            config.setUsername(databaseUsername);
            config.setPassword(databasePassword);
            config.setDriverClassName(databaseDriver);
            
            // Smaller pool for tests
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("ProductDomainTestPool");
            
            return new HikariDataSource(config);
        }

    }
}