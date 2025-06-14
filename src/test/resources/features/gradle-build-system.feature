Feature: Gradle Build System and Project Structure
  As a developer
  I want a properly configured Gradle build system with modular structure
  So that I can efficiently build and manage the ProductDomain microservice

  Background:
    Given the ProductDomain project root directory exists

  Scenario: Root Gradle project configuration
    When I examine the root build.gradle file
    Then it should have the following plugins:
      | plugin                              | version |
      | java                               | -       |
      | org.springframework.boot           | 3.2.0   |
      | io.spring.dependency-management    | 1.1.4   |
    And the Java version should be set to 21
    And the group should be "com.erpmicroservices"
    And the version should follow semantic versioning

  Scenario: Multi-module project structure
    When I examine the settings.gradle file
    Then the root project name should be "ProductDomain"
    And it should include the following modules:
      | module         |
      | database       |
      | api           |
      | ui-components |
    And each module should have its project directory configured

  Scenario: Gradle wrapper configuration
    When I check the Gradle wrapper configuration
    Then the Gradle version should be 8.5 or higher
    And the wrapper files should exist:
      | file                                    |
      | gradlew                                |
      | gradlew.bat                            |
      | gradle/wrapper/gradle-wrapper.jar      |
      | gradle/wrapper/gradle-wrapper.properties |
    And the gradlew script should be executable

  Scenario: API module configuration
    Given the api module exists
    When I examine the api/build.gradle file
    Then it should apply the Spring Boot plugin
    And it should have the following dependencies:
      | dependency                                      | scope          |
      | spring-boot-starter-web                        | implementation |
      | spring-boot-starter-graphql                    | implementation |
      | spring-boot-starter-security                   | implementation |
      | spring-boot-starter-validation                 | implementation |
      | spring-boot-starter-actuator                   | implementation |
      | spring-boot-starter-test                       | testImplementation |
    And it should depend on the database module

  Scenario: Database module configuration
    Given the database module exists
    When I examine the database/build.gradle file
    Then it should have the Flyway plugin configured
    And it should have PostgreSQL dependencies
    And it should have Spring Data JPA configured

  Scenario: UI-components module configuration
    Given the ui-components module exists
    When I examine the ui-components structure
    Then it should have a package.json file
    And it should have Vite configuration
    And it should have React dependencies
    And it should have a proper build integration with Gradle

  Scenario: Dependency version management
    When I examine the dependency management configuration
    Then all Spring Boot dependencies should use the same version
    And all modules should use consistent library versions
    And dependency versions should be managed centrally
    And there should be no version conflicts between modules

  Scenario: Build task orchestration
    When I run the root build task
    Then all modules should be built in the correct order
    And database module should be built before api module
    And all tests should pass
    And build artifacts should be generated for each module

  Scenario: Parallel build configuration
    When I examine the gradle.properties file
    Then parallel build execution should be enabled
    And build cache should be configured
    And appropriate JVM settings should be configured
    And daemon settings should be optimized

  Scenario: Quality and code analysis plugins
    When I examine the build configuration
    Then the following quality plugins should be configured:
      | plugin      | purpose                |
      | checkstyle  | Code style checking    |
      | pmd         | Static analysis        |
      | spotbugs    | Bug detection          |
      | jacoco      | Code coverage          |
    And quality checks should be part of the build lifecycle

  Scenario: Clean build from scratch
    When I run "./gradlew clean build"
    Then the build should complete successfully
    And all modules should be compiled
    And all tests should be executed
    And build reports should be generated
    And no compilation errors should occur

  Scenario: Incremental build performance
    Given a successful build has been completed
    When I make a small change to one module
    And I run the build again
    Then only the affected module should be rebuilt
    And the build should complete faster than a clean build
    And incremental compilation should be used