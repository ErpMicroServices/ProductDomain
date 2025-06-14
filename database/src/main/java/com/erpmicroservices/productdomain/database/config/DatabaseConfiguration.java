package com.erpmicroservices.productdomain.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database configuration for ProductDomain microservice.
 * 
 * Configures PostgreSQL datasource with HikariCP connection pooling
 * and Flyway database migrations.
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
     * Configure Flyway for database migrations.
     * 
     * @param dataSource the configured datasource
     * @return configured Flyway instance
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .cleanDisabled(false)
                .load();
    }

    /**
     * Test-specific datasource configuration for integration tests.
     * Uses H2 in-memory database for faster test execution.
     */
    @Configuration
    @Profile("test")
    static class TestDatabaseConfiguration {

        @Bean
        @Primary
        public DataSource testDataSource() {
            HikariConfig config = new HikariConfig();
            
            // Use PostgreSQL for tests to ensure compatibility
            // In real scenarios, you might use TestContainers
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/productdomain_test");
            config.setUsername("productdomain_test");
            config.setPassword("productdomain_test");
            config.setDriverClassName("org.postgresql.Driver");
            
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

        @Bean
        public Flyway testFlyway(DataSource testDataSource) {
            return Flyway.configure()
                    .dataSource(testDataSource)
                    .locations("classpath:db/migration", "classpath:db/test-data")
                    .baselineOnMigrate(true)
                    .cleanOnValidationError(true)
                    .cleanDisabled(false)
                    .load();
        }
    }
}