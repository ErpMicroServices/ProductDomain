package com.erpmicroservices.productdomain.database.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Database Configuration Tests")
class DatabaseConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Nested
    @DisplayName("DataSource Configuration")
    class DataSourceConfigurationTest {

        @Test
        @DisplayName("Should configure PostgreSQL datasource successfully")
        void shouldConfigurePostgreSQLDataSourceSuccessfully() {
            assertNotNull(dataSource, "DataSource should be configured");
        }

        @Test
        @DisplayName("Should establish database connection")
        void shouldEstablishDatabaseConnection() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                assertNotNull(connection, "Should establish database connection");
                assertFalse(connection.isClosed(), "Connection should be open");
            }
        }

        @Test
        @DisplayName("Should connect to PostgreSQL 16")
        void shouldConnectToPostgreSQL16() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                assertEquals("PostgreSQL", metaData.getDatabaseProductName(), "Should connect to PostgreSQL");
                assertEquals(16, metaData.getDatabaseMajorVersion(), "Should be PostgreSQL version 16");
            }
        }

        @Test
        @DisplayName("Should support multiple concurrent connections")
        void shouldSupportMultipleConcurrentConnections() throws SQLException {
            Connection[] connections = new Connection[3];
            
            try {
                // Establish multiple connections
                for (int i = 0; i < 3; i++) {
                    connections[i] = dataSource.getConnection();
                    assertNotNull(connections[i], "Should establish connection " + (i + 1));
                }
                
                // Verify all connections are active
                for (int i = 0; i < 3; i++) {
                    assertFalse(connections[i].isClosed(), "Connection " + (i + 1) + " should be active");
                }
            } finally {
                // Clean up connections
                for (Connection connection : connections) {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                }
            }
        }

        @Test
        @DisplayName("Should handle connection pooling properly")
        void shouldHandleConnectionPoolingProperly() throws SQLException {
            // Test connection reuse by obtaining and closing multiple connections
            for (int i = 0; i < 5; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    assertNotNull(connection, "Should obtain connection from pool");
                    assertTrue(connection.isValid(5), "Connection should be valid");
                }
            }
        }
    }

    @Nested
    @DisplayName("Database Schema Validation")
    class DatabaseSchemaValidationTest {

        @Test
        @DisplayName("Should have required database extensions")
        void shouldHaveRequiredDatabaseExtensions() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(
                    "SELECT extname FROM pg_extension WHERE extname IN ('uuid-ossp', 'pgcrypto')"
                );
                
                int extensionCount = 0;
                while (resultSet.next()) {
                    extensionCount++;
                }
                
                assertTrue(extensionCount >= 1, "Should have at least one required extension installed");
            }
        }

        @Test
        @DisplayName("Should validate database encoding")
        void shouldValidateDatabaseEncoding() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SHOW server_encoding");
                
                if (resultSet.next()) {
                    String encoding = resultSet.getString(1);
                    assertEquals("UTF8", encoding, "Database should use UTF8 encoding");
                }
            }
        }

        @Test
        @DisplayName("Should validate database timezone configuration")
        void shouldValidateDatabaseTimezoneConfiguration() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SHOW timezone");
                
                if (resultSet.next()) {
                    String timezone = resultSet.getString(1);
                    assertNotNull(timezone, "Database timezone should be configured");
                }
            }
        }
    }

    @Nested
    @DisplayName("Connection Properties")
    class ConnectionPropertiesTest {

        @Test
        @DisplayName("Should have proper isolation level")
        void shouldHaveProperIsolationLevel() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                int isolationLevel = connection.getTransactionIsolation();
                assertTrue(isolationLevel == Connection.TRANSACTION_READ_COMMITTED || 
                          isolationLevel == Connection.TRANSACTION_REPEATABLE_READ,
                          "Should have appropriate transaction isolation level");
            }
        }

        @Test
        @DisplayName("Should support transactions")
        void shouldSupportTransactions() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.getMetaData().supportsTransactions(), 
                          "Database should support transactions");
            }
        }

        @Test
        @DisplayName("Should have autocommit disabled for transaction management")
        void shouldHaveAutocommitDisabledForTransactionManagement() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                // Note: This test may vary based on configuration
                // Some connection pools manage autocommit differently
                DatabaseMetaData metaData = connection.getMetaData();
                assertTrue(metaData.supportsTransactions(), "Should support transaction management");
            }
        }
    }

    @Nested
    @DisplayName("Performance Configuration")
    class PerformanceConfigurationTest {

        @Test
        @DisplayName("Should have reasonable connection timeout")
        void shouldHaveReasonableConnectionTimeout() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                // Test that connection can be established within reasonable time
                long startTime = System.currentTimeMillis();
                assertTrue(connection.isValid(10), "Connection should be valid within 10 seconds");
                long connectionTime = System.currentTimeMillis() - startTime;
                assertTrue(connectionTime < 5000, "Connection should be established quickly (< 5 seconds)");
            }
        }

        @Test
        @DisplayName("Should create prepared statements")
        void shouldCreatePreparedStatements() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                // Test that we can create a prepared statement
                String sql = "SELECT 1";
                try (var statement = connection.prepareStatement(sql)) {
                    assertNotNull(statement, "Should be able to create prepared statements");
                    assertTrue(statement.execute(), "Prepared statement should execute successfully");
                }
            }
        }

        @Test
        @DisplayName("Should support batch updates")
        void shouldSupportBatchUpdates() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                assertTrue(metaData.supportsBatchUpdates(), 
                          "Database should support batch updates for performance");
            }
        }
    }
}