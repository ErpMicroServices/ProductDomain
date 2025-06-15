Feature: Product Pricing Rules
  As a pricing manager
  I want to manage complex pricing rules
  So that I can optimize revenue and offer competitive prices

  Background:
    Given the system is initialized
    And I am authenticated as a pricing manager
    And the base currency is "USD"

  @api @database @tiered-pricing
  Scenario: Volume-based tiered pricing
    Given product "SKU-BULK" has a base price of $10.00
    And the following price tiers are configured:
      | min_quantity | max_quantity | unit_price | discount_percent |
      | 1           | 9           | 10.00     | 0               |
      | 10          | 49          | 9.00      | 10              |
      | 50          | 99          | 8.00      | 20              |
      | 100         | 499         | 7.00      | 30              |
      | 500         | -           | 6.00      | 40              |
    When I calculate prices for quantities:
      | quantity | expected_unit_price | expected_total |
      | 5        | 10.00              | 50.00         |
      | 25       | 9.00               | 225.00        |
      | 75       | 8.00               | 600.00        |
      | 250      | 7.00               | 1750.00       |
      | 1000     | 6.00               | 6000.00       |
    Then the prices should match expected values

  @api @database @customer-specific
  Scenario: Customer-specific pricing
    Given the following customers with pricing agreements:
      | customer_id | customer_type | negotiated_discount |
      | CUST-001   | platinum      | 25%                |
      | CUST-002   | gold          | 15%                |
      | CUST-003   | silver        | 10%                |
      | CUST-004   | regular       | 0%                 |
    And product "SKU-CUSTOM" has a list price of $100.00
    When each customer requests the price
    Then they should see:
      | customer_id | price  | savings |
      | CUST-001   | 75.00  | 25.00   |
      | CUST-002   | 85.00  | 15.00   |
      | CUST-003   | 90.00  | 10.00   |
      | CUST-004   | 100.00 | 0.00    |

  @api @database @time-based
  Scenario: Time-based promotional pricing
    Given product "SKU-PROMO" has a regular price of $50.00
    And the following promotions are scheduled:
      | name               | start_date  | end_date    | discount_type | discount_value |
      | Weekend Sale       | 2024-01-20  | 2024-01-21  | percentage    | 20            |
      | Flash Deal         | 2024-01-25  | 2024-01-25  | fixed         | 15            |
      | Clearance         | 2024-02-01  | 2024-02-28  | percentage    | 40            |
    When I check the price on different dates:
      | date       | expected_price | active_promotion |
      | 2024-01-19 | 50.00         | none            |
      | 2024-01-20 | 40.00         | Weekend Sale    |
      | 2024-01-25 | 35.00         | Flash Deal      |
      | 2024-02-15 | 30.00         | Clearance       |
    Then the prices should reflect active promotions

  @api @database @bundle-pricing
  Scenario: Bundle discount pricing
    Given the following products:
      | sku      | name          | individual_price |
      | LAPTOP   | Laptop Pro    | 1200.00         |
      | MOUSE    | Wireless Mouse| 50.00           |
      | KEYBOARD | Keyboard Pro  | 100.00          |
      | BAG      | Laptop Bag    | 80.00           |
    And a bundle "OFFICE-BUNDLE" with discount rules:
      | products_in_bundle           | bundle_discount |
      | LAPTOP + MOUSE              | 5%             |
      | LAPTOP + MOUSE + KEYBOARD   | 10%            |
      | ALL_FOUR                    | 15%            |
    When I calculate bundle prices:
      | selected_products                    | total_before | total_after | savings |
      | LAPTOP,MOUSE                        | 1250.00     | 1187.50    | 62.50   |
      | LAPTOP,MOUSE,KEYBOARD               | 1350.00     | 1215.00    | 135.00  |
      | LAPTOP,MOUSE,KEYBOARD,BAG           | 1430.00     | 1215.50    | 214.50  |
    Then bundle prices should be correctly calculated

  @api @database @currency
  Scenario: Multi-currency pricing with exchange rates
    Given product "SKU-GLOBAL" has a USD price of $100.00
    And current exchange rates are:
      | currency | rate_to_usd |
      | EUR      | 0.85       |
      | GBP      | 0.73       |
      | JPY      | 110.50     |
      | CAD      | 1.25       |
    When I request prices in different currencies
    Then I should see:
      | currency | price   | formatted_price |
      | USD      | 100.00  | $100.00        |
      | EUR      | 85.00   | €85.00         |
      | GBP      | 73.00   | £73.00         |
      | JPY      | 11050   | ¥11,050        |
      | CAD      | 125.00  | C$125.00       |

  @api @database @dynamic-pricing
  Scenario: Dynamic pricing based on demand
    Given product "SKU-DYNAMIC" with base price $50.00
    And dynamic pricing is enabled with rules:
      | condition                    | price_adjustment |
      | inventory < 10              | +20%            |
      | inventory between 10-50     | +10%            |
      | inventory > 200             | -10%            |
      | high_demand (>100 views/hr) | +15%            |
    When the following conditions apply:
      | inventory_level | demand_rate | expected_price |
      | 5              | normal      | 60.00         |
      | 25             | normal      | 55.00         |
      | 300            | normal      | 45.00         |
      | 25             | high        | 63.25         |
    Then prices should adjust dynamically

  @api @database @price-rules-priority
  Scenario: Complex price rule prioritization
    Given product "SKU-COMPLEX" with base price $100.00
    And the following price rules exist:
      | rule_type          | value | priority |
      | volume_discount    | 10%   | 3        |
      | customer_discount  | 15%   | 2        |
      | promotion         | 20%   | 1        |
      | loyalty_points    | 5%    | 4        |
    When a platinum customer orders 50 units during a promotion
    And they have loyalty points
    Then discounts should apply in priority order:
      | step | rule_applied      | price_after |
      | 1    | promotion        | 80.00       |
      | 2    | customer_discount | 68.00       |
      | 3    | volume_discount   | 61.20       |
      | 4    | loyalty_points    | 58.14       |
    And the final price should be $58.14 per unit

  @api @database @margin-protection
  Scenario: Minimum margin protection
    Given products with cost and minimum margin requirements:
      | sku       | cost  | min_margin |
      | SKU-MARG1 | 50.00 | 20%       |
      | SKU-MARG2 | 30.00 | 30%       |
    When automatic discounts would violate margins:
      | sku       | proposed_discount | proposed_price | allowed |
      | SKU-MARG1 | 30%              | 42.00         | false   |
      | SKU-MARG1 | 15%              | 51.00         | false   |
      | SKU-MARG1 | 10%              | 54.00         | true    |
      | SKU-MARG2 | 40%              | 24.00         | false   |
    Then the system should enforce minimum margins
    And generate alerts for blocked discounts

  @api @database @competitive-pricing
  Scenario: Competitive pricing automation
    Given competitor pricing data:
      | sku       | our_price | competitor_a | competitor_b | competitor_c |
      | SKU-COMP1 | 100.00   | 95.00       | 98.00       | 102.00      |
      | SKU-COMP2 | 50.00    | 48.00       | 52.00       | 47.00       |
    And pricing strategy rules:
      | rule                           | action              |
      | if_lowest_competitor < our_price - 5% | match_lowest + 1%  |
      | if_we_are_highest              | reduce_by_2%       |
      | if_we_are_lowest               | increase_by_1%     |
    When competitive pricing runs
    Then prices should adjust to:
      | sku       | new_price | reason                    |
      | SKU-COMP1 | 95.95    | Matched lowest + 1%       |
      | SKU-COMP2 | 47.47    | Matched lowest + 1%       |

  @api @database @price-history
  Scenario: Track and analyze price history
    Given product "SKU-HISTORY" with price changes:
      | date       | price  | reason              |
      | 2024-01-01 | 100.00 | Initial price       |
      | 2024-01-15 | 90.00  | Promotion           |
      | 2024-02-01 | 95.00  | Partial increase    |
      | 2024-02-15 | 85.00  | Competitor pressure |
      | 2024-03-01 | 95.00  | Market recovery     |
    When I request price analytics
    Then I should see:
      | metric                 | value  |
      | average_price         | 93.00  |
      | price_volatility      | 5.48%  |
      | lowest_price          | 85.00  |
      | highest_price         | 100.00 |
      | current_vs_initial    | -5%    |