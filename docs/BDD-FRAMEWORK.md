# BDD/Cucumber Test Framework Documentation

## Overview

This document describes the Behavior-Driven Development (BDD) testing framework implemented for the ProductDomain microservice using Cucumber 7.x with JUnit 5 integration.

## Architecture

### Framework Components

1. **Test Runner** (`CucumberTestRunner.java`)
   - Main entry point for Cucumber tests
   - Configures JUnit Platform integration
   - Sets up report generation

2. **Hooks** (`CucumberHooks.java`)
   - Global test lifecycle management
   - Database cleanup before scenarios
   - Test context initialization
   - Transaction management for @transactional tests

3. **Test Utilities**
   - `TestDataFactory`: Generates realistic test data
   - `DatabaseCleanupUtil`: Manages database state between tests
   - `TestContextHolder`: Thread-safe context for sharing data between steps

4. **Reporting** (`CucumberReportGenerator.java`)
   - Generates HTML reports from JSON output
   - Creates custom performance and failure analysis reports

## Project Structure

```
src/test/
├── java/
│   └── com/erpmicroservices/productdomain/
│       └── bdd/
│           ├── runner/          # Test runners
│           ├── hooks/           # Cucumber hooks
│           ├── utils/           # Test utilities
│           ├── steps/           # Common step definitions
│           └── reporting/       # Report generation
└── resources/
    ├── features/               # Gherkin feature files
    │   ├── product-management/
    │   ├── inventory/
    │   └── pricing/
    ├── cucumber.properties     # Cucumber configuration
    └── junit-platform.properties
```

## Configuration

### cucumber.properties
```properties
cucumber.plugin=pretty,html:target/cucumber-reports/html-report.html,json:target/cucumber-reports/cucumber.json
cucumber.glue=com.erpmicroservices.productdomain.bdd,com.erpmicroservices.productdomain.steps
cucumber.features=src/test/resources/features
cucumber.filter.tags=not @ignore and not @manual
```

### Gradle Tasks

- `cucumberTest`: Run all Cucumber tests and generate reports
- `generateCucumberReports`: Generate HTML reports from JSON output
- `test -Dcucumber.filter.tags="@smoke"`: Run specific tagged tests

## Writing Tests

### Feature Files

Features follow Gherkin syntax:

```gherkin
Feature: Product Management
  As a product manager
  I want to manage products
  So that I can maintain the catalog

  @api @database
  Scenario: Create a new product
    Given I have product details
    When I create the product
    Then the product should be created successfully
```

### Step Definitions

```java
@Given("I have product details")
public void iHaveProductDetails() {
    Map<String, Object> product = testDataFactory.createProduct();
    testContext.put("product", product);
}

@When("I create the product")
public void iCreateTheProduct() {
    // Implementation
}

@Then("the product should be created successfully")
public void theProductShouldBeCreatedSuccessfully() {
    // Assertions
}
```

## Tags and Hooks

### Available Tags

- `@api`: API-level tests
- `@database`: Database integration tests
- `@validation`: Input validation tests
- `@performance`: Performance tests
- `@security`: Security tests
- `@smoke`: Quick smoke tests
- `@regression`: Full regression suite
- `@parallel`: Tests that can run in parallel
- `@transactional`: Tests that need transaction rollback
- `@no-cleanup`: Skip database cleanup
- `@with-data:{dataset}`: Load specific test data set

### Tagged Hooks

```java
@Before("@api")
public void setupApiContext() {
    // API-specific setup
}

@After("@performance")
public void capturePerformanceMetrics() {
    // Capture and report metrics
}
```

## Test Data Management

### Using TestDataFactory

```java
// Create a single product
Map<String, Object> product = testDataFactory.createProduct();

// Create product with overrides
Map<String, Object> customProduct = testDataFactory.createProduct(
    Map.of("name", "Custom Product", "price", new BigDecimal("99.99"))
);

// Create test catalog
Map<String, Object> catalog = testDataFactory.createProductCatalog(
    5,  // categories
    20  // products
);
```

### Database Cleanup

Tests automatically clean the database before each scenario unless tagged with `@no-cleanup`:

```java
// Clean specific tables
databaseCleanupUtil.cleanTables(Arrays.asList("products", "categories"));

// Clean all except protected tables
databaseCleanupUtil.cleanAllTablesExcept(Set.of("users", "roles"));

// Create/restore snapshots
databaseCleanupUtil.createSnapshot("before-test");
databaseCleanupUtil.restoreSnapshot("before-test");
```

## Test Context

Share data between steps using TestContextHolder:

```java
// Store data
testContext.put("orderId", orderId);
testContext.put("response", response);

// Retrieve data
String orderId = testContext.get("orderId");
Response response = testContext.getResponse();

// Track created entities for cleanup
testContext.addCreatedEntity("product", productId);
```

## Parallel Execution

Enable parallel execution in cucumber.properties:

```properties
cucumber.execution.parallel.enabled=true
cucumber.execution.parallel.config.strategy=dynamic
cucumber.execution.parallel.config.dynamic.factor=1
```

Tests must be thread-safe and use isolated data.

## CI/CD Integration

### GitHub Actions

The framework includes GitHub Actions workflow for:
- Running tests on push/PR
- Parallel test execution
- Performance test scheduling
- Report artifact upload

### Running in CI

```bash
# Run all tests
./gradlew cucumberTest

# Run specific tags
./gradlew cucumberTest -Dcucumber.filter.tags="@smoke"

# Generate reports
./gradlew generateCucumberReports
```

## Reports

### Generated Reports

1. **HTML Report**: `target/cucumber-reports/html-report.html`
2. **JSON Report**: `target/cucumber-reports/cucumber.json`
3. **JUnit XML**: `target/cucumber-reports/junit-report.xml`
4. **Timeline**: `target/cucumber-reports/timeline/`
5. **Rich HTML**: `build/cucumber-html-reports/overview-features.html`

### Report Features

- Feature overview with pass/fail statistics
- Scenario execution details
- Step timings and performance metrics
- Failure stack traces and screenshots
- Tag statistics
- Trend analysis (when history available)

## Best Practices

1. **Feature Organization**
   - Group features by domain/module
   - One feature per file
   - Clear, business-focused scenarios

2. **Step Definitions**
   - Keep steps simple and reusable
   - Use TestContext for data sharing
   - Avoid UI-specific steps in API tests

3. **Test Data**
   - Use TestDataFactory for consistency
   - Clean up created data
   - Avoid hard-coded values

4. **Performance**
   - Tag slow tests appropriately
   - Use parallel execution for independent tests
   - Monitor test execution times

5. **Maintenance**
   - Regular cleanup of obsolete tests
   - Keep step definitions DRY
   - Document complex test scenarios

## Troubleshooting

### Common Issues

1. **Database connection errors**
   - Ensure PostgreSQL is running
   - Check connection properties
   - Verify test database exists

2. **Parallel execution failures**
   - Check for shared state
   - Ensure proper test isolation
   - Use separate schemas/databases

3. **Report generation fails**
   - Verify JSON reports exist
   - Check file permissions
   - Ensure dependencies are correct

### Debug Mode

Enable detailed logging:

```properties
logging.level.com.erpmicroservices.productdomain.bdd=DEBUG
logging.level.io.cucumber=DEBUG
```

## Examples

See the `src/test/resources/features` directory for comprehensive examples covering:
- Product CRUD operations
- Category management
- Inventory tracking
- Pricing rules
- Bundle management
- Performance testing