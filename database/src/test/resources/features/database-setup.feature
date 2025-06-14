Feature: PostgreSQL Database Schema and Configuration
  As a developer
  I want to have a properly configured PostgreSQL database with schema
  So that I can store and manage product domain data

  Background:
    Given a clean PostgreSQL 16 database instance

  Scenario: Database connection and basic configuration
    When I connect to the database
    Then the connection should be successful
    And the database should be PostgreSQL version 16
    And the database should have proper connection pooling configured

  Scenario: Core tables are created with proper schema
    Given the database migrations have been applied
    When I query the database schema
    Then the following tables should exist:
      | table_name          |
      | products           |
      | categories         |
      | product_variants   |
      | product_categories |
    And all tables should have proper primary keys
    And all tables should have audit columns (created_at, updated_at)

  Scenario: Product table structure and constraints
    Given the database migrations have been applied
    When I examine the products table
    Then it should have the following columns:
      | column_name    | data_type | constraints           |
      | id             | UUID      | PRIMARY KEY, NOT NULL |
      | name           | VARCHAR   | NOT NULL              |
      | description    | TEXT      | NULL                  |
      | sku            | VARCHAR   | UNIQUE, NOT NULL      |
      | price          | DECIMAL   | NOT NULL              |
      | status         | VARCHAR   | NOT NULL              |
      | created_at     | TIMESTAMP | NOT NULL              |
      | updated_at     | TIMESTAMP | NOT NULL              |
      | created_by     | UUID      | NOT NULL              |
      | updated_by     | UUID      | NOT NULL              |
    And it should have proper indexes for performance

  Scenario: Category table structure and hierarchy support
    Given the database migrations have been applied
    When I examine the categories table
    Then it should have the following columns:
      | column_name    | data_type | constraints           |
      | id             | UUID      | PRIMARY KEY, NOT NULL |
      | name           | VARCHAR   | NOT NULL              |
      | description    | TEXT      | NULL                  |
      | parent_id      | UUID      | FOREIGN KEY           |
      | path           | VARCHAR   | NOT NULL              |
      | level          | INTEGER   | NOT NULL              |
      | created_at     | TIMESTAMP | NOT NULL              |
      | updated_at     | TIMESTAMP | NOT NULL              |
      | created_by     | UUID      | NOT NULL              |
      | updated_by     | UUID      | NOT NULL              |
    And it should support hierarchical category structure

  Scenario: Product variants table for product variations
    Given the database migrations have been applied
    When I examine the product_variants table
    Then it should have the following columns:
      | column_name    | data_type | constraints           |
      | id             | UUID      | PRIMARY KEY, NOT NULL |
      | product_id     | UUID      | FOREIGN KEY, NOT NULL |
      | name           | VARCHAR   | NOT NULL              |
      | sku            | VARCHAR   | UNIQUE, NOT NULL      |
      | price          | DECIMAL   | NULL                  |
      | attributes     | JSONB     | NULL                  |
      | stock_quantity | INTEGER   | NOT NULL              |
      | created_at     | TIMESTAMP | NOT NULL              |
      | updated_at     | TIMESTAMP | NOT NULL              |
      | created_by     | UUID      | NOT NULL              |
      | updated_by     | UUID      | NOT NULL              |

  Scenario: Many-to-many relationship between products and categories
    Given the database migrations have been applied
    When I examine the product_categories table
    Then it should have the following columns:
      | column_name | data_type | constraints           |
      | product_id  | UUID      | FOREIGN KEY, NOT NULL |
      | category_id | UUID      | FOREIGN KEY, NOT NULL |
      | created_at  | TIMESTAMP | NOT NULL              |
      | created_by  | UUID      | NOT NULL              |
    And it should have a composite primary key on (product_id, category_id)

  Scenario: Database indexes for performance optimization
    Given the database migrations have been applied
    When I check the database indexes
    Then the following indexes should exist:
      | table_name         | index_name                    | columns                |
      | products          | idx_products_sku              | sku                    |
      | products          | idx_products_status           | status                 |
      | products          | idx_products_created_at       | created_at             |
      | categories        | idx_categories_parent_id      | parent_id              |
      | categories        | idx_categories_path           | path                   |
      | product_variants  | idx_variants_product_id       | product_id             |
      | product_variants  | idx_variants_sku              | sku                    |
      | product_categories| idx_product_categories_prod   | product_id             |
      | product_categories| idx_product_categories_cat    | category_id            |

  Scenario: Flyway migration versioning and management
    Given Flyway is properly configured
    When I check the flyway migration status
    Then all migrations should be applied successfully
    And the flyway_schema_history table should exist
    And migration versions should be sequential and valid

  Scenario: Database connection pooling and performance
    Given the database connection pool is configured
    When I test multiple concurrent connections
    Then the connection pool should handle multiple connections efficiently
    And connections should be properly released after use
    And pool metrics should be available for monitoring