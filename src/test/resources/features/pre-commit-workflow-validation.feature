Feature: Pre-commit GitHub Actions Workflow Validation
  As a developer
  I want pre-commit hooks to validate GitHub Actions workflows
  So that deprecated actions and workflow issues are caught before committing

  Background:
    Given the ProductDomain project exists
    And pre-commit hooks are installed

  Scenario: Pre-commit detects deprecated GitHub Actions versions
    Given a workflow file with deprecated action versions
    When I attempt to commit the changes
    Then the pre-commit hook should fail
    And it should report the deprecated actions
    And it should suggest the latest versions

  Scenario: Pre-commit validates workflow YAML syntax
    Given a workflow file with invalid YAML syntax
    When I attempt to commit the changes
    Then the pre-commit hook should fail
    And it should report the YAML syntax errors
    And it should indicate the line numbers with issues

  Scenario: Pre-commit checks for required workflow properties
    Given a workflow file missing required properties
    When I attempt to commit the changes
    Then the pre-commit hook should fail
    And it should list the missing required properties
    And it should provide examples of valid workflow structure

  Scenario: Pre-commit allows valid workflows with latest actions
    Given a workflow file with all latest action versions
    And valid YAML syntax
    And all required properties
    When I attempt to commit the changes
    Then the pre-commit hook should pass
    And the commit should proceed

  Scenario: Pre-commit checks multiple workflow files
    Given multiple workflow files in .github/workflows
    And some have deprecated actions
    When I attempt to commit the changes
    Then the pre-commit hook should check all workflow files
    And it should report issues for each file separately
    And it should fail if any file has issues

  Scenario: Pre-commit validates action version formats
    Given a workflow file with malformed action versions
    When I attempt to commit the changes
    Then the pre-commit hook should fail
    And it should report invalid version formats
    And it should show the correct version format

  Scenario: Pre-commit caches action version lookups
    Given the pre-commit hook has checked action versions before
    When I attempt to commit without workflow changes
    Then the pre-commit hook should use cached version data
    And it should complete quickly
    And the cache should expire after 24 hours

  Scenario: Pre-commit handles third-party actions
    Given a workflow file with third-party actions
    When I attempt to commit the changes
    Then the pre-commit hook should validate third-party action versions
    And it should check if newer versions exist
    And it should warn about unmaintained actions

  Scenario: Pre-commit validates Docker actions
    Given a workflow file using Docker-based actions
    When I attempt to commit the changes
    Then the pre-commit hook should validate Docker action syntax
    And it should check Docker image tags
    And it should warn about using 'latest' tags

  Scenario: Pre-commit provides fix suggestions
    Given a workflow file with deprecated actions
    When the pre-commit hook fails
    Then it should provide automated fix commands
    And it should show a diff of suggested changes
    And it should offer to apply fixes automatically