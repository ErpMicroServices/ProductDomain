Feature: GraphQL Category Management API
  As a client application
  I want to use GraphQL to manage categories
  So that I can organize products hierarchically

  Background:
    Given the GraphQL API is available
    And test categories are loaded in the database

  @graphql @query
  Scenario: Query category hierarchy
    Given a category hierarchy exists
    When I execute the GraphQL query:
      """
      query GetCategoryHierarchy($id: UUID!) {
        category(id: $id) {
          id
          name
          description
          parent {
            id
            name
          }
          children {
            id
            name
            children {
              id
              name
            }
          }
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
      """
    With variables:
      | id | 550e8400-e29b-41d4-a716-446655440010 |
    Then the GraphQL response should be successful
    And the category should have nested children
    And the category should contain products

  @graphql @query
  Scenario: Query root categories
    Given multiple category hierarchies exist
    When I execute the GraphQL query:
      """
      query GetRootCategories {
        categories(filter: { isRoot: true }) {
          edges {
            node {
              id
              name
              childrenCount
            }
          }
          totalCount
        }
      }
      """
    Then the GraphQL response should be successful
    And all returned categories should have no parent
    And each category should show its children count

  @graphql @mutation
  Scenario: Create category with parent
    Given a parent category exists with ID "550e8400-e29b-41d4-a716-446655440011"
    When I execute the GraphQL mutation:
      """
      mutation CreateCategory($input: CreateCategoryInput!) {
        createCategory(input: $input) {
          id
          name
          description
          parent {
            id
            name
          }
        }
      }
      """
    With variables:
      | input.name        | Subcategory Test           |
      | input.description | A test subcategory         |
      | input.parentId    | 550e8400-e29b-41d4-a716-446655440011 |
    Then the GraphQL response should be successful
    And the created category should have the specified parent

  @graphql @mutation
  Scenario: Move category to different parent
    Given a category exists with ID "550e8400-e29b-41d4-a716-446655440012"
    And a new parent category exists with ID "550e8400-e29b-41d4-a716-446655440013"
    When I execute the GraphQL mutation:
      """
      mutation MoveCategory($id: UUID!, $newParentId: UUID) {
        moveCategory(id: $id, newParentId: $newParentId) {
          id
          parent {
            id
            name
          }
        }
      }
      """
    With variables:
      | id          | 550e8400-e29b-41d4-a716-446655440012 |
      | newParentId | 550e8400-e29b-41d4-a716-446655440013 |
    Then the GraphQL response should be successful
    And the category parent should be updated

  @graphql @validation
  Scenario: Prevent circular category reference
    Given a category hierarchy exists with parent and child
    When I execute the GraphQL mutation to move parent under child:
      """
      mutation MoveCategory($id: UUID!, $newParentId: UUID) {
        moveCategory(id: $id, newParentId: $newParentId) {
          id
        }
      }
      """
    Then the GraphQL response should contain an error
    And the error message should contain "circular reference"