Feature: Category Management
  As a catalog manager
  I want to organize products into categories
  So that customers can easily navigate and find products

  Background:
    Given the system is initialized
    And I am authenticated as a catalog manager

  @api @database
  Scenario: Create category hierarchy
    Given I create a root category "Electronics"
    When I add subcategories:
      | parent      | name              | description                    |
      | Electronics | Computers         | Desktop and laptop computers   |
      | Electronics | Mobile Devices    | Phones and tablets            |
      | Computers   | Laptops          | Portable computers            |
      | Computers   | Desktops         | Desktop computers             |
      | Computers   | Accessories      | Computer accessories          |
    Then the category tree should have 3 levels
    And "Electronics" should have 2 direct children
    And "Computers" should have 3 direct children

  @api @database
  Scenario: Move category to different parent
    Given the following category structure exists:
      """
      Home & Garden
        ├── Kitchen
        │   ├── Appliances
        │   └── Cookware
        └── Bedroom
            └── Furniture
      """
    When I move "Appliances" to "Home & Garden"
    Then "Appliances" should be a direct child of "Home & Garden"
    And "Kitchen" should have only "Cookware" as child

  @api @validation
  Scenario: Prevent circular category references
    Given categories "A", "B", and "C" exist
    And "B" is a child of "A"
    And "C" is a child of "B"
    When I try to make "A" a child of "C"
    Then the operation should fail with error "Circular reference detected"
    And the category hierarchy should remain unchanged

  @api @database
  Scenario: Assign multiple categories to product
    Given the following categories exist:
      | name        | path                    |
      | Summer      | Seasonal/Summer         |
      | Beachwear   | Clothing/Beachwear      |
      | Sale Items  | Promotions/Sale Items   |
    And a product "Beach Shorts" exists
    When I assign the product to categories:
      | Summer      |
      | Beachwear   |
      | Sale Items  |
    Then the product should appear in all 3 categories
    And the product should be findable through any category path

  @api @database
  Scenario: Bulk reassign products between categories
    Given category "Old Electronics" has 500 products
    And category "Modern Electronics" exists
    When I bulk move all products from "Old Electronics" to "Modern Electronics"
    Then "Modern Electronics" should have 500 products
    And "Old Electronics" should have 0 products
    And the operation should complete within 5 seconds

  @api @database @cascade
  Scenario: Delete category with cascade options
    Given a category "Discontinued" with 50 products
    And the category has 3 subcategories with products
    When I delete the category with cascade option "reassign"
    And I specify "Clearance" as the target category
    Then all 50 products should be moved to "Clearance"
    And all subcategory products should be moved to "Clearance"
    And the "Discontinued" category tree should be removed

  @api @seo
  Scenario: Generate SEO-friendly category URLs
    Given I create categories with names:
      | name                           | expected_slug                  |
      | Kitchen & Dining              | kitchen-dining                 |
      | Children's Toys (Age 3-5)     | childrens-toys-age-3-5        |
      | Bücher & Zeitschriften        | bucher-zeitschriften          |
      | 50% Off Sale!!                | 50-off-sale                   |
    Then each category should have the expected SEO-friendly slug

  @api @database @performance
  Scenario: Efficiently retrieve category path
    Given a deeply nested category structure with 10 levels
    And each level has 5 categories
    When I request the full path for a leaf category
    Then the response should include all parent categories
    And the query should execute in less than 100ms

  @api @database
  Scenario: Category attribute inheritance
    Given a parent category "Clothing" with attributes:
      | attribute    | value        | inheritable |
      | return_days  | 30          | true        |
      | size_chart   | standard    | true        |
      | wash_care    | required    | false       |
    When I create a subcategory "T-Shirts"
    Then "T-Shirts" should inherit attributes:
      | attribute    | value     |
      | return_days  | 30        |
      | size_chart   | standard  |
    But not inherit "wash_care"

  @api @database @analytics
  Scenario: Track category performance metrics
    Given categories with the following activity:
      | category    | views | clicks | conversions |
      | Electronics | 10000 | 2000   | 150        |
      | Clothing    | 8000  | 1800   | 200        |
      | Home        | 5000  | 800    | 50         |
    When I request category analytics for last month
    Then I should see conversion rates:
      | category    | conversion_rate |
      | Electronics | 7.5%           |
      | Clothing    | 11.1%          |
      | Home        | 6.25%          |
    And categories should be ranked by performance