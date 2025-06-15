package com.erpmicroservices.productdomain.bdd.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for database cleanup in tests.
 * Provides methods for truncating tables, resetting sequences, and managing test data.
 */
@Component
public class DatabaseCleanupUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupUtil.class);
    
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private JdbcTemplate jdbcTemplate;
    
    // Tables that should never be cleaned
    private static final Set<String> PROTECTED_TABLES = Set.of(
        "flyway_schema_history",
        "databasechangelog",
        "databasechangeloglock"
    );

    public DatabaseCleanupUtil(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Package-private setter for testing
    void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Clean specified tables in the correct order to handle foreign key constraints.
     */
    public void cleanTables(List<String> tables) {
        executeInTransaction(() -> {
            try {
                disableConstraints();
                
                for (String table : tables) {
                    cleanTable(table);
                }
                
                enableConstraints();
            } catch (Exception e) {
                throw new DatabaseCleanupException("Failed to clean tables", e);
            }
        });
    }

    /**
     * Clean all tables except the specified ones.
     */
    public void cleanAllTablesExcept(Set<String> excludedTables) {
        Set<String> allExcluded = new HashSet<>(PROTECTED_TABLES);
        allExcluded.addAll(excludedTables);
        
        try {
            List<String> tablesToClean = getAllTables().stream()
                .filter(table -> !allExcluded.contains(table.toLowerCase()))
                .collect(Collectors.toList());
            
            cleanTables(tablesToClean);
        } catch (Exception e) {
            throw new DatabaseCleanupException("Failed to clean all tables", e);
        }
    }

    /**
     * Reset sequences to a specific value.
     */
    public void resetSequences(List<String> sequences, long startValue) {
        for (String sequence : sequences) {
            try {
                jdbcTemplate.execute(String.format("ALTER SEQUENCE %s RESTART WITH %d", sequence, startValue));
                logger.debug("Reset sequence {} to {}", sequence, startValue);
            } catch (Exception e) {
                logger.warn("Failed to reset sequence {}: {}", sequence, e.getMessage());
            }
        }
    }

    /**
     * Reset all sequences in the database.
     */
    public void resetAllSequences() {
        try {
            List<String> sequences = getAllSequences();
            resetSequences(sequences, 1L);
        } catch (Exception e) {
            throw new DatabaseCleanupException("Failed to reset all sequences", e);
        }
    }

    /**
     * Execute a cleanup operation within a transaction.
     */
    public void executeInTransaction(Runnable operation) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            operation.run();
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new DatabaseCleanupException("Transaction failed", e);
        }
    }

    /**
     * Create a snapshot of current database state.
     */
    public void createSnapshot(String snapshotName) {
        executeInTransaction(() -> {
            try {
                List<String> tables = getAllTables();
                
                for (String table : tables) {
                    if (!PROTECTED_TABLES.contains(table.toLowerCase())) {
                        String snapshotTable = snapshotName + "_" + table;
                        jdbcTemplate.execute(String.format(
                            "CREATE TABLE %s AS SELECT * FROM %s", 
                            snapshotTable, table
                        ));
                        logger.debug("Created snapshot table: {}", snapshotTable);
                    }
                }
            } catch (Exception e) {
                throw new DatabaseCleanupException("Failed to create snapshot", e);
            }
        });
    }

    /**
     * Restore database from a snapshot.
     */
    public void restoreSnapshot(String snapshotName) {
        executeInTransaction(() -> {
            try {
                disableConstraints();
                
                List<String> snapshotTables = getSnapshotTables(snapshotName);
                
                for (String snapshotTable : snapshotTables) {
                    String originalTable = snapshotTable.substring(snapshotName.length() + 1);
                    
                    cleanTable(originalTable);
                    jdbcTemplate.execute(String.format(
                        "INSERT INTO %s SELECT * FROM %s",
                        originalTable, snapshotTable
                    ));
                    logger.debug("Restored table {} from snapshot", originalTable);
                }
                
                enableConstraints();
            } catch (Exception e) {
                throw new DatabaseCleanupException("Failed to restore snapshot", e);
            }
        });
    }

    /**
     * Delete a snapshot.
     */
    public void deleteSnapshot(String snapshotName) {
        executeInTransaction(() -> {
            try {
                List<String> snapshotTables = getSnapshotTables(snapshotName);
                
                for (String table : snapshotTables) {
                    jdbcTemplate.execute("DROP TABLE " + table);
                    logger.debug("Deleted snapshot table: {}", table);
                }
            } catch (Exception e) {
                throw new DatabaseCleanupException("Failed to delete snapshot", e);
            }
        });
    }

    /**
     * Check if a table exists.
     */
    public boolean tableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking if table exists: {}", tableName, e);
            return false;
        }
    }

    /**
     * Get row count for a table.
     */
    public long getRowCount(String tableName) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName, 
            Long.class
        );
    }

    /**
     * Disable foreign key constraints.
     */
    public void disableConstraints() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        logger.debug("Disabled referential integrity");
    }

    /**
     * Enable foreign key constraints.
     */
    public void enableConstraints() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        logger.debug("Enabled referential integrity");
    }

    /**
     * Get foreign key relationships for a table.
     */
    public List<ForeignKeyRelation> getForeignKeyRelations(String tableName) {
        List<ForeignKeyRelation> relations = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            try (ResultSet rs = metaData.getImportedKeys(null, null, tableName)) {
                while (rs.next()) {
                    ForeignKeyRelation relation = new ForeignKeyRelation(
                        rs.getString("FKTABLE_NAME"),
                        rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME")
                    );
                    relations.add(relation);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting foreign key relations for table: {}", tableName, e);
        }
        
        return relations;
    }

    /**
     * Perform batch delete with configurable batch size.
     */
    public int batchDelete(String table, String condition, Object[] params, int batchSize) {
        int totalDeleted = 0;
        int deleted;
        
        do {
            String sql = String.format(
                "DELETE FROM %s WHERE %s AND ROWNUM <= ?",
                table, condition
            );
            
            Object[] batchParams = Arrays.copyOf(params, params.length + 1);
            batchParams[params.length] = batchSize;
            
            deleted = jdbcTemplate.update(sql, batchParams);
            totalDeleted += deleted;
            
            logger.debug("Batch deleted {} rows from {}", deleted, table);
        } while (deleted > 0);
        
        return totalDeleted;
    }

    /**
     * Vacuum tables to reclaim space.
     */
    public void vacuumTables(List<String> tables) {
        for (String table : tables) {
            try {
                jdbcTemplate.execute("VACUUM " + table);
                logger.debug("Vacuumed table: {}", table);
            } catch (Exception e) {
                logger.warn("Failed to vacuum table {}: {}", table, e.getMessage());
            }
        }
    }

    // Private helper methods

    private void cleanTable(String table) {
        jdbcTemplate.execute("TRUNCATE TABLE " + table);
        logger.debug("Truncated table: {}", table);
    }

    private List<String> getAllTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        
        return tables;
    }

    private List<String> getAllSequences() throws SQLException {
        List<String> sequences = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"SEQUENCE"})) {
                while (rs.next()) {
                    sequences.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        
        return sequences;
    }

    private List<String> getSnapshotTables(String snapshotName) {
        return jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE ?",
            String.class,
            snapshotName + "_%"
        );
    }

    /**
     * Represents a foreign key relationship.
     */
    public static class ForeignKeyRelation {
        private final String fromTable;
        private final String fromColumn;
        private final String toTable;
        private final String toColumn;

        public ForeignKeyRelation(String fromTable, String fromColumn, String toTable, String toColumn) {
            this.fromTable = fromTable;
            this.fromColumn = fromColumn;
            this.toTable = toTable;
            this.toColumn = toColumn;
        }

        public String getFromTable() { return fromTable; }
        public String getFromColumn() { return fromColumn; }
        public String getToTable() { return toTable; }
        public String getToColumn() { return toColumn; }
    }
}

/**
 * Custom exception for database cleanup operations.
 */
class DatabaseCleanupException extends RuntimeException {
    public DatabaseCleanupException(String message) {
        super(message);
    }

    public DatabaseCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}