Feature: GitHub Actions CI/CD Pipeline
  As a development team
  I want a comprehensive CI/CD pipeline
  So that code quality and deployments are automated and reliable

  Background:
    Given the ProductDomain project exists
    And GitHub Actions is configured

  Scenario: Main CI workflow runs on pull requests
    Given a pull request is created
    When the pull request is opened or updated
    Then the main CI workflow should trigger
    And it should run code quality checks
    And it should run all unit tests
    And it should run all integration tests
    And it should generate test reports
    And it should post results to the pull request

  Scenario: Code quality gates enforce standards
    Given a pull request with code changes
    When the quality checks run
    Then Checkstyle should verify code formatting
    And PMD should check for code issues
    And SpotBugs should analyze for bugs
    And the build should fail if quality gates are not met
    And detailed reports should be available

  Scenario: Security scanning detects vulnerabilities
    Given a project with dependencies
    When the security scan runs
    Then OWASP dependency check should analyze all dependencies
    And it should identify known vulnerabilities
    And it should fail the build for high severity issues
    And it should generate a security report

  Scenario: Docker images are built and published
    Given a successful build on main branch
    When the Docker build workflow runs
    Then it should build the API Docker image
    And it should build the UI Docker image
    And it should tag images with version and commit SHA
    And it should push images to the registry
    And it should scan images for vulnerabilities

  Scenario: Deployment workflow handles environments
    Given Docker images are available
    When a deployment is triggered
    Then it should validate the target environment
    And it should deploy to the correct environment
    And it should run smoke tests after deployment
    And it should rollback on failure
    And it should notify the team of results

  Scenario: Dependency updates are automated
    Given the project has dependencies
    When Dependabot runs
    Then it should check for dependency updates
    And it should create pull requests for updates
    And it should group related updates
    And it should include changelogs
    And automated tests should validate updates

  Scenario: Branch protection rules are enforced
    Given a protected branch
    When changes are pushed
    Then direct pushes should be blocked
    And pull requests should be required
    And status checks must pass before merging
    And at least one approval should be required
    And the branch history should be linear

  Scenario: Multi-job workflow with matrix builds
    Given a workflow with matrix strategy
    When the workflow runs
    Then it should test on multiple Java versions
    And it should test on multiple operating systems
    And jobs should run in parallel
    And results should be aggregated
    And any failure should fail the workflow

  Scenario: Test results are properly reported
    Given tests have been executed
    When test reporting runs
    Then JUnit test results should be parsed
    And test reports should be published
    And failed tests should be annotated
    And coverage reports should be generated
    And trends should be tracked over time

  Scenario: Artifacts are managed for deployments
    Given a successful build
    When artifact management runs
    Then build artifacts should be uploaded
    And artifacts should be versioned
    And retention policies should be applied
    And artifacts should be downloadable
    And deployment artifacts should be signed