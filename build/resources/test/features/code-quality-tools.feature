Feature: Code Quality Tools and Standards
  As a developer
  I want comprehensive code quality tools configured
  So that code quality is maintained across the ProductDomain project

  Background:
    Given the ProductDomain project is set up

  Scenario: Checkstyle configuration and execution
    When I examine the Checkstyle configuration
    Then Checkstyle should be configured with custom rules
    And the Checkstyle configuration file should exist at "config/checkstyle/checkstyle.xml"
    And Checkstyle should check for:
      | rule_category          | description                           |
      | Naming Conventions     | Consistent naming for classes/methods |
      | Imports                | No wildcard or unused imports        |
      | Whitespace            | Proper spacing and indentation       |
      | Size Violations       | Method and file length limits        |
      | Coding Problems       | Common coding issues                 |
      | Class Design          | Proper class structure               |
    When I run "./gradlew checkstyleMain"
    Then the task should complete successfully if code follows standards
    And violations should be reported in "build/reports/checkstyle/main.html"

  Scenario: PMD static analysis configuration
    When I examine the PMD configuration
    Then PMD should be configured with custom ruleset
    And the PMD ruleset file should exist at "config/pmd/ruleset.xml"
    And PMD should check for:
      | rule_category      | description                        |
      | Best Practices     | Java best practices               |
      | Code Style        | Consistent code styling           |
      | Design            | Design quality issues             |
      | Error Prone       | Potentially buggy code            |
      | Performance       | Performance issues                |
      | Security          | Security vulnerabilities          |
    When I run "./gradlew pmdMain"
    Then the task should complete successfully if no violations exist
    And violations should be reported in "build/reports/pmd/main.html"

  Scenario: SpotBugs bug detection configuration
    When I examine the SpotBugs configuration
    Then SpotBugs should be configured for bug detection
    And the SpotBugs exclude filter should exist at "config/spotbugs/exclude.xml"
    And SpotBugs should check for:
      | bug_category          | description                      |
      | Correctness          | Probable bugs                    |
      | Performance          | Performance issues               |
      | Security             | Security vulnerabilities         |
      | Bad Practice         | Violations of best practices     |
      | Malicious Code       | Vulnerability to malicious code  |
    When I run "./gradlew spotbugsMain"
    Then the task should complete successfully if no bugs found
    And bug reports should be generated in "build/reports/spotbugs/main.html"

  Scenario: OWASP Dependency Check configuration
    When I examine the OWASP Dependency Check configuration
    Then OWASP Dependency Check plugin should be configured
    And it should scan for:
      | vulnerability_type    | description                           |
      | Known CVEs           | Common Vulnerabilities and Exposures  |
      | Outdated Dependencies| Dependencies with newer versions      |
      | License Issues       | Problematic licenses                  |
    When I run "./gradlew dependencyCheckAnalyze"
    Then the task should scan all project dependencies
    And vulnerability reports should be generated in "build/reports/dependency-check-report.html"
    And the build should fail if critical vulnerabilities are found

  Scenario: ESLint configuration for React components
    When I examine the ui-components module
    Then ESLint should be configured with:
      | configuration        | description                    |
      | React rules         | React-specific linting rules   |
      | TypeScript rules    | TypeScript linting rules       |
      | Accessibility rules | a11y compliance rules          |
      | Hook rules          | React hooks best practices     |
    And the ESLint config should exist at "ui-components/.eslintrc.cjs"
    When I run "./gradlew ui-components:npmLint"
    Then the task should check all TypeScript and React files
    And linting errors should fail the build

  Scenario: JaCoCo code coverage configuration
    When I examine the JaCoCo configuration
    Then JaCoCo should be configured for code coverage
    And coverage thresholds should be set:
      | metric              | minimum |
      | Line Coverage       | 80%     |
      | Branch Coverage     | 70%     |
      | Method Coverage     | 80%     |
      | Class Coverage      | 90%     |
    When I run "./gradlew test jacocoTestReport"
    Then coverage reports should be generated in "build/reports/jacoco/test/html/index.html"
    And the build should fail if coverage is below thresholds

  Scenario: Quality gates in build process
    When I run "./gradlew check"
    Then all quality checks should run in order:
      | check_type     | description                         |
      | test          | Unit tests                          |
      | checkstyle    | Code style violations               |
      | pmd           | Static analysis                     |
      | spotbugs      | Bug detection                       |
      | jacoco        | Code coverage                       |
    And the build should fail if any quality gate fails
    And all reports should be generated in the build directory

  Scenario: Pre-commit hooks configuration
    When I examine the pre-commit hooks setup
    Then pre-commit hooks should be configured for:
      | hook_type          | action                              |
      | format-check      | Check code formatting               |
      | lint              | Run linting checks                  |
      | test              | Run related unit tests              |
      | compile           | Ensure code compiles                |
    And hooks should be automatically installed on project setup
    And commits should be blocked if hooks fail

  Scenario: Aggregated quality report
    When I run "./gradlew qualityReport"
    Then an aggregated quality report should be generated
    And it should include:
      | report_section     | content                            |
      | Test Results      | Unit and integration test results   |
      | Coverage          | Code coverage metrics               |
      | Style Violations  | Checkstyle violations               |
      | Code Smells       | PMD violations                      |
      | Bugs              | SpotBugs findings                   |
      | Dependencies      | Vulnerability scan results          |
    And the report should be available at "build/reports/quality/index.html"

  Scenario: IDE integration for quality tools
    When I import the project into an IDE
    Then quality tool configurations should be recognized
    And IDE should show:
      | integration        | feature                            |
      | Checkstyle        | Real-time style checking           |
      | PMD               | Code analysis warnings             |
      | SpotBugs          | Bug detection in editor            |
      | ESLint            | JavaScript/TypeScript linting      |
    And code formatting should match project standards

  Scenario: Continuous quality monitoring
    When quality checks are integrated with CI/CD
    Then each pull request should:
      | action            | description                         |
      | Run all checks   | Execute all quality tools           |
      | Report status    | Show pass/fail for each check       |
      | Block merge      | Prevent merge if checks fail        |
      | Show trends      | Display quality metrics over time   |
    And quality metrics should be tracked over time