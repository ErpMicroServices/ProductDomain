package com.erpmicroservices.productdomain.bdd.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Database Cleanup Utility Tests")
class DatabaseCleanupUtilTest {

    @Mock
    private DataSource dataSource;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @Mock
    private PlatformTransactionManager transactionManager;
    
    @Mock
    private Connection connection;
    
    @Mock
    private DatabaseMetaData metaData;
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private TransactionStatus transactionStatus;
    
    private DatabaseCleanupUtil cleanupUtil;

    @BeforeEach
    void setUp() {
        cleanupUtil = new DatabaseCleanupUtil(dataSource, transactionManager);
        cleanupUtil.setJdbcTemplate(jdbcTemplate); // For testing
    }

    @Nested
    @DisplayName("Table Cleanup Tests")
    class TableCleanupTests {

        @Test
        @DisplayName("Should clean specified tables in correct order")
        void shouldCleanSpecifiedTables() throws Exception {
            // Given
            List<String> tables = Arrays.asList("order_items", "orders", "products");
            
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);

            // When
            cleanupUtil.cleanTables(tables);

            // Then
            verify(jdbcTemplate).execute("SET REFERENTIAL_INTEGRITY FALSE");
            verify(jdbcTemplate).execute("TRUNCATE TABLE order_items");
            verify(jdbcTemplate).execute("TRUNCATE TABLE orders");
            verify(jdbcTemplate).execute("TRUNCATE TABLE products");
            verify(jdbcTemplate).execute("SET REFERENTIAL_INTEGRITY TRUE");
            verify(transactionManager).commit(transactionStatus);
        }

        @Test
        @DisplayName("Should handle cleanup errors gracefully")
        void shouldHandleCleanupErrors() {
            // Given
            List<String> tables = Arrays.asList("invalid_table");
            
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
            when(jdbcTemplate.execute(anyString())).thenThrow(
                new RuntimeException("Table not found")
            );

            // When/Then
            assertThatThrownBy(() -> cleanupUtil.cleanTables(tables))
                .isInstanceOf(DatabaseCleanupException.class)
                .hasMessageContaining("Failed to clean tables");
                
            verify(transactionManager).rollback(transactionStatus);
        }

        @Test
        @DisplayName("Should clean all tables except excluded ones")
        void shouldCleanAllTablesExceptExcluded() throws Exception {
            // Given
            Set<String> excludedTables = Set.of("flyway_schema_history", "users");
            
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getTables(any(), any(), any(), any())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, true, false);
            when(resultSet.getString("TABLE_NAME"))
                .thenReturn("products", "flyway_schema_history", "orders");

            // When
            cleanupUtil.cleanAllTablesExcept(excludedTables);

            // Then
            verify(jdbcTemplate).execute("TRUNCATE TABLE products");
            verify(jdbcTemplate).execute("TRUNCATE TABLE orders");
            verify(jdbcTemplate, never()).execute("TRUNCATE TABLE flyway_schema_history");
            verify(jdbcTemplate, never()).execute("TRUNCATE TABLE users");
        }
    }

    @Nested
    @DisplayName("Sequence Reset Tests")
    class SequenceResetTests {

        @Test
        @DisplayName("Should reset sequences to specified value")
        void shouldResetSequences() {
            // Given
            List<String> sequences = Arrays.asList("product_seq", "order_seq");
            
            // When
            cleanupUtil.resetSequences(sequences, 1L);

            // Then
            verify(jdbcTemplate).execute("ALTER SEQUENCE product_seq RESTART WITH 1");
            verify(jdbcTemplate).execute("ALTER SEQUENCE order_seq RESTART WITH 1");
        }

        @Test
        @DisplayName("Should reset all sequences")
        void shouldResetAllSequences() throws Exception {
            // Given
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getTables(any(), any(), any(), eq(new String[]{"SEQUENCE"})))
                .thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, true, false);
            when(resultSet.getString("TABLE_NAME"))
                .thenReturn("product_seq", "order_seq");

            // When
            cleanupUtil.resetAllSequences();

            // Then
            verify(jdbcTemplate).execute("ALTER SEQUENCE product_seq RESTART WITH 1");
            verify(jdbcTemplate).execute("ALTER SEQUENCE order_seq RESTART WITH 1");
        }
    }

    @Nested
    @DisplayName("Transaction Management Tests")
    class TransactionManagementTests {

        @Test
        @DisplayName("Should execute cleanup in transaction")
        void shouldExecuteCleanupInTransaction() {
            // Given
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
            
            // When
            cleanupUtil.executeInTransaction(() -> {
                jdbcTemplate.execute("DELETE FROM products");
            });

            // Then
            verify(transactionManager).getTransaction(any());
            verify(transactionManager).commit(transactionStatus);
        }

        @Test
        @DisplayName("Should rollback transaction on error")
        void shouldRollbackTransactionOnError() {
            // Given
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
            RuntimeException error = new RuntimeException("Cleanup failed");
            
            // When/Then
            assertThatThrownBy(() -> 
                cleanupUtil.executeInTransaction(() -> {
                    throw error;
                })
            ).isInstanceOf(DatabaseCleanupException.class);
            
            verify(transactionManager).rollback(transactionStatus);
        }
    }

    @Nested
    @DisplayName("Snapshot and Restore Tests")
    class SnapshotRestoreTests {

        @Test
        @DisplayName("Should create database snapshot")
        void shouldCreateDatabaseSnapshot() {
            // Given
            String snapshotName = "test_snapshot";
            List<String> tables = Arrays.asList("products", "categories");
            
            when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(tables);

            // When
            cleanupUtil.createSnapshot(snapshotName);

            // Then
            verify(jdbcTemplate).execute("CREATE TABLE test_snapshot_products AS SELECT * FROM products");
            verify(jdbcTemplate).execute("CREATE TABLE test_snapshot_categories AS SELECT * FROM categories");
        }

        @Test
        @DisplayName("Should restore from snapshot")
        void shouldRestoreFromSnapshot() {
            // Given
            String snapshotName = "test_snapshot";
            List<String> snapshotTables = Arrays.asList(
                "test_snapshot_products",
                "test_snapshot_categories"
            );
            
            when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(snapshotTables);

            // When
            cleanupUtil.restoreSnapshot(snapshotName);

            // Then
            verify(jdbcTemplate).execute("TRUNCATE TABLE products");
            verify(jdbcTemplate).execute("INSERT INTO products SELECT * FROM test_snapshot_products");
            verify(jdbcTemplate).execute("TRUNCATE TABLE categories");
            verify(jdbcTemplate).execute("INSERT INTO categories SELECT * FROM test_snapshot_categories");
        }

        @Test
        @DisplayName("Should delete snapshot")
        void shouldDeleteSnapshot() {
            // Given
            String snapshotName = "test_snapshot";
            List<String> snapshotTables = Arrays.asList(
                "test_snapshot_products",
                "test_snapshot_categories"
            );
            
            when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(snapshotTables);

            // When
            cleanupUtil.deleteSnapshot(snapshotName);

            // Then
            verify(jdbcTemplate).execute("DROP TABLE test_snapshot_products");
            verify(jdbcTemplate).execute("DROP TABLE test_snapshot_categories");
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should check if table exists")
        void shouldCheckIfTableExists() throws Exception {
            // Given
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getTables(any(), any(), eq("products"), any()))
                .thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // When
            boolean exists = cleanupUtil.tableExists("products");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should get table row count")
        void shouldGetTableRowCount() {
            // Given
            when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class))
                .thenReturn(42L);

            // When
            long count = cleanupUtil.getRowCount("products");

            // Then
            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should disable and enable constraints")
        void shouldDisableAndEnableConstraints() {
            // When
            cleanupUtil.disableConstraints();
            cleanupUtil.enableConstraints();

            // Then
            verify(jdbcTemplate).execute("SET REFERENTIAL_INTEGRITY FALSE");
            verify(jdbcTemplate).execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        @Test
        @DisplayName("Should get foreign key relationships")
        void shouldGetForeignKeyRelationships() throws Exception {
            // Given
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getImportedKeys(any(), any(), eq("order_items")))
                .thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString("FKTABLE_NAME")).thenReturn("order_items");
            when(resultSet.getString("PKTABLE_NAME")).thenReturn("orders");

            // When
            List<DatabaseCleanupUtil.ForeignKeyRelation> relations = 
                cleanupUtil.getForeignKeyRelations("order_items");

            // Then
            assertThat(relations).hasSize(1);
            assertThat(relations.get(0).getFromTable()).isEqualTo("order_items");
            assertThat(relations.get(0).getToTable()).isEqualTo("orders");
        }
    }

    @Nested
    @DisplayName("Batch Operation Tests")
    class BatchOperationTests {

        @Test
        @DisplayName("Should perform batch delete")
        void shouldPerformBatchDelete() {
            // Given
            String table = "products";
            String condition = "created_at < ?";
            Object[] params = new Object[]{"2023-01-01"};
            int batchSize = 1000;

            when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(1000, 500, 0);

            // When
            int totalDeleted = cleanupUtil.batchDelete(table, condition, params, batchSize);

            // Then
            assertThat(totalDeleted).isEqualTo(1500);
            verify(jdbcTemplate, times(3)).update(
                contains("DELETE FROM products"),
                any(Object[].class)
            );
        }

        @Test
        @DisplayName("Should vacuum tables after cleanup")
        void shouldVacuumTables() {
            // Given
            List<String> tables = Arrays.asList("products", "categories");

            // When
            cleanupUtil.vacuumTables(tables);

            // Then
            verify(jdbcTemplate).execute("VACUUM products");
            verify(jdbcTemplate).execute("VACUUM categories");
        }
    }
}