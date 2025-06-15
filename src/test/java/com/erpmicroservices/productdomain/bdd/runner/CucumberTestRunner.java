package com.erpmicroservices.productdomain.bdd.runner;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Main Cucumber test runner for the ProductDomain BDD tests.
 * Configures Cucumber with JUnit Platform for test execution.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty," +
                "html:target/cucumber-reports/html-report.html," +
                "json:target/cucumber-reports/cucumber.json," +
                "junit:target/cucumber-reports/junit-report.xml," +
                "timeline:target/cucumber-reports/timeline"),
    @ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
        value = "com.erpmicroservices.productdomain.bdd," +
                "com.erpmicroservices.productdomain.steps," +
                "com.erpmicroservices.productdomain.api.steps," +
                "com.erpmicroservices.productdomain.database.steps"),
    @ConfigurationParameter(key = FEATURES_PROPERTY_NAME, 
        value = "src/test/resources/features"),
    @ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, 
        value = "not @ignore and not @manual"),
    @ConfigurationParameter(key = JUNIT_PLATFORM_NAMING_STRATEGY_PROPERTY_NAME, 
        value = "long"),
    @ConfigurationParameter(key = PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME, 
        value = "false"),
    @ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, 
        value = "true"),
    @ConfigurationParameter(key = SNIPPET_TYPE_PROPERTY_NAME, 
        value = "camelcase"),
    @ConfigurationParameter(key = EXECUTION_DRY_RUN_PROPERTY_NAME, 
        value = "false")
})
public class CucumberTestRunner {
    // This class serves as the entry point for Cucumber tests
    // No implementation needed - configuration is done via annotations
}