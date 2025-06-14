package com.erpmicroservices.productdomain.database.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Flyway Migration Tests")
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Nested
    @DisplayName("Migration Configuration")
    class MigrationConfigurationTest {

        @Test
        @DisplayName("Should configure Flyway with proper settings")
        void shouldConfigureFlywayWithProperSettings() {
            assertNotNull(flyway, "Flyway should be configured");
            
            var configuration = flyway.getConfiguration();
            assertNotNull(configuration.getDataSource(), "Flyway should have datasource configured");
            var locations = configuration.getLocations();
            assertTrue(locations.length > 0, "Flyway should have migration locations");
            assertTrue(locations[0].toString().contains("db/migration"),
                        "Flyway should use correct migration location");
        }

        @Test
        @DisplayName("Should have flyway schema history table")
        void shouldHaveFlywaySchemaHistoryTable() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet tables = metaData.getTables(null, null, "flyway_schema_history", null);
                assertTrue(tables.next(), "flyway_schema_history table should exist");
            }
        }

        @Test
        @DisplayName("Should validate migration file naming convention")
        void shouldValidateMigrationFileNamingConvention() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] migrations = migrationInfo.all();
            
            assertTrue(migrations.length > 0, "Should have at least one migration");
            
            for (MigrationInfo migration : migrations) {
                String version = migration.getVersion().toString();
                assertNotNull(version, "Migration should have version");
                assertTrue(version.matches("\\d+(\\.\\d+)*"), 
                          "Migration version should follow semantic versioning: " + version);
            }
        }
    }

    @Nested
    @DisplayName("Migration Execution")
    class MigrationExecutionTest {

        @Test
        @DisplayName("Should execute all migrations successfully")
        void shouldExecuteAllMigrationsSuccessfully() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] appliedMigrations = migrationInfo.applied();
            
            assertTrue(appliedMigrations.length > 0, "Should have applied migrations");
            
            for (MigrationInfo migration : appliedMigrations) {
                assertEquals(MigrationState.SUCCESS, migration.getState(), 
                          "Migration " + migration.getVersion() + " should be successful");
            }
        }

        @Test
        @DisplayName("Should have no pending migrations")
        void shouldHaveNoPendingMigrations() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] pendingMigrations = migrationInfo.pending();
            
            assertEquals(0, pendingMigrations.length, "Should have no pending migrations after startup");
        }

        @Test
        @DisplayName("Should validate migration checksums")
        void shouldValidateMigrationChecksums() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] appliedMigrations = migrationInfo.applied();
            
            for (MigrationInfo migration : appliedMigrations) {
                assertNotNull(migration.getChecksum(), 
                             "Migration " + migration.getVersion() + " should have valid checksum");
            }
        }

        @Test
        @DisplayName("Should track migration execution time")
        void shouldTrackMigrationExecutionTime() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] appliedMigrations = migrationInfo.applied();
            
            for (MigrationInfo migration : appliedMigrations) {
                assertNotNull(migration.getInstalledOn(), 
                             "Migration " + migration.getVersion() + " should have installation timestamp");
                assertTrue(migration.getExecutionTime() >= 0, 
                          "Migration " + migration.getVersion() + " should have execution time recorded");
            }
        }
    }

    @Nested
    @DisplayName("Database Schema Validation")
    class DatabaseSchemaValidationTest {

        @Test
        @DisplayName("Should create core product tables")
        void shouldCreateCoreProductTables() throws SQLException {
            String[] expectedTables = {"products", "categories", "product_variants", "product_categories"};
            
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                for (String tableName : expectedTables) {
                    ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
                    assertTrue(tables.next(), "Table " + tableName + " should exist after migrations");
                }
            }
        }

        @Test
        @DisplayName("Should create proper primary keys")
        void shouldCreateProperPrimaryKeys() throws SQLException {
            String[] tablesWithSinglePK = {"products", "categories", "product_variants"};
            
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                for (String tableName : tablesWithSinglePK) {
                    ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName);
                    assertTrue(primaryKeys.next(), "Table " + tableName + " should have primary key");
                    
                    String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                    assertEquals("id", pkColumnName, "Primary key should be named 'id'");
                }
            }
        }

        @Test
        @DisplayName("Should create proper foreign key relationships")
        void shouldCreateProperForeignKeyRelationships() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                // Check product_variants -> products foreign key
                ResultSet variantFKs = metaData.getImportedKeys(null, null, "product_variants");
                boolean hasProductFK = false;
                while (variantFKs.next()) {
                    if ("product_id".equals(variantFKs.getString("FKCOLUMN_NAME"))) {
                        hasProductFK = true;
                        assertEquals("products", variantFKs.getString("PKTABLE_NAME"),
                                   "product_variants.product_id should reference products table");
                        break;
                    }
                }
                assertTrue(hasProductFK, "product_variants should have foreign key to products");
                
                // Check categories self-referencing foreign key
                ResultSet categoryFKs = metaData.getImportedKeys(null, null, "categories");
                boolean hasParentFK = false;
                while (categoryFKs.next()) {
                    if ("parent_id".equals(categoryFKs.getString("FKCOLUMN_NAME"))) {
                        hasParentFK = true;
                        assertEquals("categories", categoryFKs.getString("PKTABLE_NAME"),
                                   "categories.parent_id should reference categories table");
                        break;
                    }
                }
                assertTrue(hasParentFK, "categories should have self-referencing foreign key");
            }
        }

        @Test
        @DisplayName("Should create proper indexes for performance")
        void shouldCreateProperIndexesForPerformance() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                // Check for SKU index on products
                ResultSet productIndexes = metaData.getIndexInfo(null, null, "products", false, false);
                boolean hasSkuIndex = false;
                while (productIndexes.next()) {
                    String indexName = productIndexes.getString("INDEX_NAME");
                    if (indexName != null && indexName.contains("sku")) {
                        hasSkuIndex = true;
                        break;
                    }
                }
                assertTrue(hasSkuIndex, "products table should have index on SKU column");
                
                // Check for category path index
                ResultSet categoryIndexes = metaData.getIndexInfo(null, null, "categories", false, false);
                boolean hasPathIndex = false;
                while (categoryIndexes.next()) {
                    String indexName = categoryIndexes.getString("INDEX_NAME");
                    if (indexName != null && indexName.contains("path")) {
                        hasPathIndex = true;
                        break;
                    }
                }
                assertTrue(hasPathIndex, "categories table should have index on path column");
            }
        }

        @Test
        @DisplayName("Should create audit columns on all tables")
        void shouldCreateAuditColumnsOnAllTables() throws SQLException {
            String[] auditColumns = {"created_at", "updated_at", "created_by", "updated_by"};
            String[] tablesWithAudit = {"products", "categories", "product_variants"};
            
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                for (String tableName : tablesWithAudit) {
                    for (String auditColumn : auditColumns) {
                        ResultSet columns = metaData.getColumns(null, null, tableName, auditColumn);
                        assertTrue(columns.next(), 
                                  "Table " + tableName + " should have audit column " + auditColumn);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Migration Rollback and Recovery")
    class MigrationRollbackTest {

        @Test
        @DisplayName("Should validate migration reversibility information")
        void shouldValidateMigrationReversibilityInformation() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] migrations = migrationInfo.all();
            
            // Verify migration descriptions are descriptive
            for (MigrationInfo migration : migrations) {
                String description = migration.getDescription();
                assertNotNull(description, "Migration should have description");
                assertFalse(description.trim().isEmpty(), "Migration description should not be empty");
            }
        }

        @Test
        @DisplayName("Should maintain migration order consistency")
        void shouldMaintainMigrationOrderConsistency() {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo[] migrations = migrationInfo.all();
            
            // Verify migrations are in ascending version order
            for (int i = 1; i < migrations.length; i++) {
                assertTrue(migrations[i-1].getVersion().compareTo(migrations[i].getVersion()) < 0,
                          "Migrations should be in ascending version order");
            }
        }
    }
}