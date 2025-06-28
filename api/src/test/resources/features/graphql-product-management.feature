Feature: GraphQL Product Management API
  As a client application
  I want to use GraphQL to manage products
  So that I can efficiently query and manipulate product data

  Background:
    Given the GraphQL API is available
    And test data is loaded in the database

  @graphql @query
  Scenario: Query a single product by ID
    Given a product exists with ID "550e8400-e29b-41d4-a716-446655440001"
    When I execute the GraphQL query:
      """
      query GetProduct($id: UUID!) {
        product(id: $id) {
          id
          sku
          name
          description
          active
          createdAt
          categories {
            id
            name
          }
          variants {
            id
            name
            sku
            price
          }
        }
      }
      """
    Then the GraphQL response should be successful
    And the response should contain product with SKU "TEST-PROD-001"

  @graphql @query
  Scenario: Query products with pagination
    Given 25 products exist in the database
    When I execute the GraphQL query:
      """
      query ListProducts($first: Int, $after: String) {
        products(first: $first, after: $after) {
          edges {
            node {
              id
              sku
              name
            }
            cursor
          }
          pageInfo {
            hasNextPage
            hasPreviousPage
            startCursor
            endCursor
          }
          totalCount
        }
      }
      """
    With variables:
      | first | 10 |
    Then the GraphQL response should be successful
    And the response should contain 10 products
    And pageInfo.hasNextPage should be true
    And totalCount should be 25

  @graphql @query
  Scenario: Query products with filtering
    Given products exist with various categories
    When I execute the GraphQL query:
      """
      query FilterProducts($filter: ProductFilter) {
        products(filter: $filter) {
          edges {
            node {
              id
              sku
              name
              categories {
                name
              }
            }
          }
          totalCount
        }
      }
      """
    With variables:
      | filter.categoryIds | ["550e8400-e29b-41d4-a716-446655440002"] |
    Then the GraphQL response should be successful
    And all returned products should have the specified category

  @graphql @mutation
  Scenario: Create a new product
    Given I have valid product data
    When I execute the GraphQL mutation:
      """
      mutation CreateProduct($input: CreateProductInput!) {
        createProduct(input: $input) {
          id
          sku
          name
          description
          active
          createdAt
        }
      }
      """
    With variables:
      | input.sku         | NEW-PROD-001        |
      | input.name        | New Test Product    |
      | input.description | A test product      |
      | input.active      | true                |
    Then the GraphQL response should be successful
    And the created product should have SKU "NEW-PROD-001"
    And the product should be persisted in the database

  @graphql @mutation
  Scenario: Update an existing product
    Given a product exists with ID "550e8400-e29b-41d4-a716-446655440003"
    When I execute the GraphQL mutation:
      """
      mutation UpdateProduct($id: UUID!, $input: UpdateProductInput!) {
        updateProduct(id: $id, input: $input) {
          id
          name
          description
          updatedAt
        }
      }
      """
    With variables:
      | id                | 550e8400-e29b-41d4-a716-446655440003 |
      | input.name        | Updated Product Name                  |
      | input.description | Updated description                   |
    Then the GraphQL response should be successful
    And the product name should be "Updated Product Name"
    And the updatedAt timestamp should be recent

  @graphql @mutation
  Scenario: Delete a product
    Given a product exists with ID "550e8400-e29b-41d4-a716-446655440004"
    When I execute the GraphQL mutation:
      """
      mutation DeleteProduct($id: UUID!) {
        deleteProduct(id: $id)
      }
      """
    With variables:
      | id | 550e8400-e29b-41d4-a716-446655440004 |
    Then the GraphQL response should be successful
    And the response should be true
    And the product should not exist in the database

  @graphql @mutation
  Scenario: Add product to category
    Given a product exists with ID "550e8400-e29b-41d4-a716-446655440005"
    And a category exists with ID "550e8400-e29b-41d4-a716-446655440006"
    When I execute the GraphQL mutation:
      """
      mutation AddToCategory($productId: UUID!, $categoryId: UUID!) {
        addProductToCategory(productId: $productId, categoryId: $categoryId) {
          id
          categories {
            id
            name
          }
        }
      }
      """
    With variables:
      | productId  | 550e8400-e29b-41d4-a716-446655440005 |
      | categoryId | 550e8400-e29b-41d4-a716-446655440006 |
    Then the GraphQL response should be successful
    And the product should have the specified category

  @graphql @subscription
  Scenario: Subscribe to product updates
    Given I subscribe to product updates:
      """
      subscription OnProductUpdated($productId: UUID!) {
        productUpdated(productId: $productId) {
          id
          name
          updatedAt
        }
      }
      """
    With variables:
      | productId | 550e8400-e29b-41d4-a716-446655440007 |
    When the product is updated with name "Real-time Update"
    Then I should receive a subscription update
    And the update should contain the new name "Real-time Update"

  @graphql @error
  Scenario: Handle invalid product ID
    When I execute the GraphQL query:
      """
      query GetProduct($id: UUID!) {
        product(id: $id) {
          id
          name
        }
      }
      """
    With variables:
      | id | 550e8400-e29b-41d4-a716-999999999999 |
    Then the GraphQL response should contain an error
    And the error message should contain "Product not found"

  @graphql @validation
  Scenario: Validate product input
    When I execute the GraphQL mutation:
      """
      mutation CreateProduct($input: CreateProductInput!) {
        createProduct(input: $input) {
          id
          sku
        }
      }
      """
    With variables:
      | input.sku         | |
      | input.name        | |
      | input.description | Test |
    Then the GraphQL response should contain validation errors
    And the validation errors should include "SKU is required"
    And the validation errors should include "Name is required"