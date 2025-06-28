Feature: GraphQL Data Loader Performance
  As a GraphQL API
  I want to use data loaders to batch database queries
  So that I can prevent N+1 query problems

  Background:
    Given the GraphQL API is available
    And data loaders are configured
    And performance monitoring is enabled

  @graphql @performance
  Scenario: Batch load categories for multiple products
    Given 20 products exist with various categories
    When I execute the GraphQL query:
      """
      query GetProductsWithCategories {
        products(first: 20) {
          edges {
            node {
              id
              name
              categories {
                id
                name
              }
            }
          }
        }
      }
      """
    Then the GraphQL response should be successful
    And the total database queries should be less than 5
    And categories should be loaded in batches

  @graphql @performance
  Scenario: Batch load products for multiple categories
    Given 10 categories exist with various products
    When I execute the GraphQL query:
      """
      query GetCategoriesWithProducts {
        categories(first: 10) {
          edges {
            node {
              id
              name
              products {
                edges {
                  node {
                    id
                    sku
                    name
                  }
                }
              }
            }
          }
        }
      }
      """
    Then the GraphQL response should be successful
    And the total database queries should be less than 5
    And products should be loaded in batches

  @graphql @performance
  Scenario: Nested data loader efficiency
    Given a complex product hierarchy exists
    When I execute the GraphQL query:
      """
      query ComplexProductQuery {
        products(first: 10) {
          edges {
            node {
              id
              name
              categories {
                id
                name
                parent {
                  id
                  name
                }
                products {
                  edges {
                    node {
                      id
                      sku
                      variants {
                        id
                        sku
                      }
                    }
                  }
                }
              }
              variants {
                id
                sku
                attributes
              }
            }
          }
        }
      }
      """
    Then the GraphQL response should be successful
    And the query execution time should be under 100ms
    And no N+1 queries should be detected

  @graphql @performance
  Scenario: Data loader cache effectiveness
    Given products with shared categories exist
    When I execute multiple queries for the same data:
      """
      query GetProduct($id: UUID!) {
        product(id: $id) {
          id
          categories {
            id
            name
          }
        }
      }
      """
    Then subsequent queries should use cached data
    And database query count should not increase