Feature: Product CRUD Operations
  As a product manager
  I want to manage products in the system
  So that I can maintain the product catalog

  Background:
    Given the system is initialized
    And I am authenticated as a product manager

  @api @database
  Scenario: Create a new product successfully
    Given I have the following product details:
      | name        | Premium Coffee Maker      |
      | description | High-end coffee maker     |
      | sku         | SKU-COFFEE-001           |
      | price       | 299.99                   |
      | category    | Kitchen Appliances       |
    When I create the product
    Then the product should be created successfully
    And the product should have a unique ID
    And the product should be findable by SKU "SKU-COFFEE-001"

  @api @database
  Scenario: Create product with variants
    Given I have a product "T-Shirt" with SKU "SKU-TSHIRT-001"
    And the product has the following variants:
      | name    | color | size | price | stock |
      | Red S   | Red   | S    | 19.99 | 100   |
      | Red M   | Red   | M    | 19.99 | 150   |
      | Red L   | Red   | L    | 19.99 | 120   |
      | Blue S  | Blue  | S    | 19.99 | 80    |
      | Blue M  | Blue  | M    | 19.99 | 200   |
      | Blue L  | Blue  | L    | 19.99 | 90    |
    When I create the product with variants
    Then the product should have 6 variants
    And each variant should have unique SKU

  @api @validation
  Scenario: Validate required fields when creating product
    Given I have an incomplete product without required fields
    When I try to create the product
    Then I should get a validation error
    And the error should mention the missing fields:
      | name  |
      | sku   |
      | price |

  @api @database
  Scenario: Update existing product
    Given a product exists with SKU "SKU-UPDATE-001"
    When I update the product with:
      | name        | Updated Product Name |
      | description | New description      |
      | price       | 149.99              |
    Then the product should be updated successfully
    And the product history should show the changes

  @api @database @soft-delete
  Scenario: Soft delete a product
    Given a product exists with SKU "SKU-DELETE-001"
    And the product has active orders
    When I delete the product
    Then the product should be marked as inactive
    But the product data should still exist in the database
    And the product should not appear in public listings

  @api @database
  Scenario: Search products by various criteria
    Given the following products exist:
      | name                | category    | price  | tags           |
      | Laptop Pro         | Electronics | 1299   | laptop,premium |
      | Laptop Basic       | Electronics | 599    | laptop,budget  |
      | Gaming Mouse       | Electronics | 79.99  | gaming,mouse   |
      | Ergonomic Keyboard | Electronics | 129.99 | keyboard,ergo  |
      | Coffee Maker       | Appliances  | 199.99 | coffee,kitchen |
    When I search for products with:
      | criteria      | value       |
      | category      | Electronics |
      | priceRange    | 50-150      |
    Then I should get 2 products in the results
    And the results should contain "Gaming Mouse" and "Ergonomic Keyboard"

  @api @database @bulk
  Scenario: Bulk import products from CSV
    Given I have a CSV file "products.csv" with 100 products
    When I import the products
    Then 100 products should be created
    And I should receive an import summary with:
      | successful | 100 |
      | failed     | 0   |
      | skipped    | 0   |

  @api @database @performance
  Scenario: Handle large product catalog efficiently
    Given a catalog with 10000 products exists
    When I request page 50 with 20 items per page
    Then the response should return within 500ms
    And I should get exactly 20 products
    And the pagination metadata should be correct

  @api @validation @security
  Scenario: Prevent SQL injection in product search
    Given products exist in the system
    When I search with malicious input "'; DROP TABLE products; --"
    Then the search should handle the input safely
    And no data should be compromised
    And I should get empty results

  @api @database
  Scenario Outline: Product status transitions
    Given a product in "<initial_status>" status
    When I change the status to "<new_status>"
    Then the transition should <result>
    And the product status should be "<final_status>"

    Examples:
      | initial_status | new_status    | result  | final_status  |
      | draft         | published     | succeed | published     |
      | published     | archived      | succeed | archived      |
      | archived      | published     | succeed | published     |
      | draft         | archived      | fail    | draft         |
      | archived      | draft         | fail    | archived      |