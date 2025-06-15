Feature: Inventory Tracking
  As an inventory manager
  I want to track product stock levels accurately
  So that I can prevent overselling and maintain optimal inventory

  Background:
    Given the system is initialized
    And I am authenticated as an inventory manager
    And the following warehouses exist:
      | code | name            | location     | type         |
      | WH01 | Main Warehouse  | New York     | fulfillment  |
      | WH02 | West Coast Hub  | Los Angeles  | fulfillment  |
      | WH03 | Returns Center  | Chicago      | returns      |

  @api @database @real-time
  Scenario: Real-time inventory updates
    Given product "SKU-12345" has stock levels:
      | warehouse | available | reserved | damaged |
      | WH01      | 100      | 10       | 2       |
      | WH02      | 50       | 5        | 0       |
    When an order is placed for 15 units from "WH01"
    Then the inventory should be updated immediately:
      | warehouse | available | reserved |
      | WH01      | 100      | 25       |
    And total available stock should be 150
    And the inventory event should be logged

  @api @database @concurrent
  Scenario: Handle concurrent inventory updates
    Given product "SKU-CONCURRENT" has 10 units available in "WH01"
    When 3 orders are placed simultaneously for:
      | order_id | quantity |
      | ORD-001  | 4        |
      | ORD-002  | 4        |
      | ORD-003  | 4        |
    Then only 2 orders should succeed
    And 1 order should fail with "Insufficient inventory"
    And the final available stock should be 2

  @api @database @reservation
  Scenario: Inventory reservation timeout
    Given product "SKU-TIMEOUT" has 20 units available
    And the reservation timeout is set to 30 minutes
    When I reserve 15 units for order "ORD-TIMEOUT"
    And 31 minutes pass without confirmation
    Then the reservation should be automatically released
    And available stock should return to 20
    And a timeout event should be logged

  @api @database @transfer
  Scenario: Inter-warehouse inventory transfer
    Given the following inventory state:
      | product    | WH01_stock | WH02_stock |
      | SKU-TRANS1 | 100        | 20         |
      | SKU-TRANS2 | 50         | 0          |
    When I initiate a transfer:
      | product    | quantity | from | to   |
      | SKU-TRANS1 | 30      | WH01 | WH02 |
      | SKU-TRANS2 | 50      | WH01 | WH02 |
    Then the transfer should be in "in_transit" status
    And inventory should show:
      | product    | WH01_available | WH01_in_transit | WH02_available | WH02_incoming |
      | SKU-TRANS1 | 70            | 30              | 20            | 30           |
      | SKU-TRANS2 | 0             | 50              | 0             | 50           |

  @api @database @cycle-count
  Scenario: Cycle count adjustment
    Given a cycle count is scheduled for "WH01"
    And the system shows:
      | product | system_count |
      | SKU-001 | 100         |
      | SKU-002 | 50          |
      | SKU-003 | 75          |
    When I submit physical counts:
      | product | physical_count | variance_reason        |
      | SKU-001 | 98            | Damaged items found    |
      | SKU-002 | 52            | Misplaced items found  |
      | SKU-003 | 75            | -                      |
    Then inventory adjustments should be created:
      | product | adjustment | status   |
      | SKU-001 | -2        | approved |
      | SKU-002 | +2        | approved |
    And variance reports should be generated

  @api @database @low-stock
  Scenario: Low stock alerts and reorder points
    Given products with reorder configurations:
      | product | reorder_point | reorder_quantity | lead_time_days |
      | SKU-LOW | 20           | 100             | 7              |
      | SKU-MED | 50           | 200             | 14             |
    When inventory levels drop to:
      | product | current_stock |
      | SKU-LOW | 19           |
      | SKU-MED | 45           |
    Then a low stock alert should be triggered for "SKU-LOW"
    And a reorder suggestion should be created:
      | product | suggested_quantity | suggested_date |
      | SKU-LOW | 100              | today          |
    But no alert for "SKU-MED"

  @api @database @allocation
  Scenario: Smart inventory allocation across warehouses
    Given an order for 50 units of "SKU-ALLOC"
    And inventory is distributed as:
      | warehouse | available | distance_km | shipping_cost |
      | WH01      | 30       | 100        | 10.00        |
      | WH02      | 40       | 500        | 25.00        |
      | WH03      | 25       | 300        | 18.00        |
    When I request optimal allocation
    Then the system should allocate:
      | warehouse | allocated | reason                    |
      | WH01      | 30       | Closest warehouse         |
      | WH03      | 20       | Next closest with stock   |
    And estimated shipping cost should be 16.40

  @api @database @returns
  Scenario: Process inventory returns
    Given a return is initiated for order "ORD-RETURN"
    With items:
      | product    | quantity | condition      |
      | SKU-RET001 | 2       | good          |
      | SKU-RET002 | 1       | damaged       |
      | SKU-RET003 | 1       | defective     |
    When the return is received at "WH03"
    Then inventory should be updated:
      | product    | location | available | damaged | quarantine |
      | SKU-RET001 | WH03    | +2       | 0      | 0         |
      | SKU-RET002 | WH03    | 0        | +1     | 0         |
      | SKU-RET003 | WH03    | 0        | 0      | +1        |

  @api @database @bundle
  Scenario: Bundle inventory tracking
    Given a bundle "BUNDLE-001" contains:
      | component  | quantity |
      | SKU-COMP1  | 2       |
      | SKU-COMP2  | 1       |
      | SKU-COMP3  | 3       |
    And component stock levels are:
      | component  | available |
      | SKU-COMP1  | 100      |
      | SKU-COMP2  | 40       |
      | SKU-COMP3  | 150      |
    When I check bundle availability
    Then maximum bundle quantity should be 40
    And the limiting component should be "SKU-COMP2"

  @api @database @audit
  Scenario: Inventory audit trail
    Given product "SKU-AUDIT" starting with 100 units
    When the following transactions occur:
      | timestamp | type        | quantity | user      | reference  |
      | 09:00    | sale        | -10     | system    | ORD-001   |
      | 10:30    | return      | +2      | system    | RET-001   |
      | 14:00    | adjustment  | -5      | john_doe  | ADJ-001   |
      | 16:00    | transfer    | -20     | jane_doe  | TRN-001   |
    Then the audit trail should show all transactions
    And running balance should be:
      | after_transaction | balance |
      | sale             | 90      |
      | return           | 92      |
      | adjustment       | 87      |
      | transfer         | 67      |
    And the current stock should be 67