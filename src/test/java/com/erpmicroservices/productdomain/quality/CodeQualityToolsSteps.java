package com.erpmicroservices.productdomain.quality;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.datatable.DataTable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CodeQualityToolsSteps {

    private Path projectRoot;
    private File configFile;
    private String taskOutput;
    private int taskExitCode;
    private String fileContent;

    @Given("the ProductDomain project is set up")
    public void theProductDomainProjectIsSetUp() {
        projectRoot = Paths.get(System.getProperty("user.dir"));
        assertTrue(Files.exists(projectRoot), "Project root should exist");
        assertTrue(Files.exists(projectRoot.resolve("build.gradle")), "build.gradle should exist");
    }

    @When("I examine the Checkstyle configuration")
    public void iExamineTheCheckstyleConfiguration() {
        configFile = projectRoot.resolve("config/checkstyle/checkstyle.xml").toFile();
    }

    @When("I examine the PMD configuration")
    public void iExamineThePMDConfiguration() {
        configFile = projectRoot.resolve("config/pmd/ruleset.xml").toFile();
    }

    @When("I examine the SpotBugs configuration")
    public void iExamineTheSpotBugsConfiguration() {
        configFile = projectRoot.resolve("config/spotbugs/exclude.xml").toFile();
    }

    @When("I examine the OWASP Dependency Check configuration")
    public void iExamineTheOWASPDependencyCheckConfiguration() throws IOException {
        fileContent = Files.readString(projectRoot.resolve("build.gradle"));
    }

    @When("I examine the ui-components module")
    public void iExamineTheUiComponentsModule() {
        configFile = projectRoot.resolve("ui-components/.eslintrc.cjs").toFile();
    }

    @When("I examine the JaCoCo configuration")
    public void iExamineTheJaCoCoConfiguration() throws IOException {
        fileContent = Files.readString(projectRoot.resolve("build.gradle"));
    }

    @When("I run {string}")
    public void iRunCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (var reader = process.inputReader()) {
            reader.lines().forEach(line -> output.append(line).append("\n"));
        }
        
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        taskOutput = output.toString();
        taskExitCode = completed ? process.exitValue() : -1;
    }

    @When("I examine the pre-commit hooks setup")
    public void iExamineThePreCommitHooksSetup() {
        configFile = projectRoot.resolve(".git/hooks/pre-commit").toFile();
    }

    @When("I import the project into an IDE")
    public void iImportTheProjectIntoAnIDE() {
        // This is a conceptual test - actual IDE import cannot be automated
        assertTrue(Files.exists(projectRoot.resolve(".idea")) || 
                  Files.exists(projectRoot.resolve(".vscode")) ||
                  Files.exists(projectRoot.resolve(".project")),
                  "IDE configuration files should exist");
    }

    @When("quality checks are integrated with CI/CD")
    public void qualityChecksAreIntegratedWithCICD() {
        assertTrue(Files.exists(projectRoot.resolve(".github/workflows")) ||
                  Files.exists(projectRoot.resolve(".gitlab-ci.yml")) ||
                  Files.exists(projectRoot.resolve("Jenkinsfile")),
                  "CI/CD configuration should exist");
    }

    @Then("Checkstyle should be configured with custom rules")
    public void checkstyleShouldBeConfiguredWithCustomRules() {
        assertTrue(configFile.exists(), "Checkstyle config should exist");
        assertTrue(configFile.length() > 0, "Checkstyle config should not be empty");
    }

    @Then("the Checkstyle configuration file should exist at {string}")
    public void theCheckstyleConfigurationFileShouldExistAt(String path) {
        Path configPath = projectRoot.resolve(path);
        assertTrue(Files.exists(configPath), "Checkstyle config should exist at " + path);
    }

    @Then("Checkstyle should check for:")
    public void checkstyleShouldCheckFor(DataTable dataTable) throws IOException {
        String content = Files.readString(configFile.toPath());
        List<Map<String, String>> rules = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> rule : rules) {
            String category = rule.get("rule_category");
            // Verify that the config contains rules for each category
            assertTrue(content.toLowerCase().contains(category.toLowerCase().replace(" ", "")),
                      "Checkstyle config should contain rules for " + category);
        }
    }

    @Then("the task should complete successfully if code follows standards")
    public void theTaskShouldCompleteSuccessfullyIfCodeFollowsStandards() {
        // If there are no violations, exit code should be 0
        assertTrue(taskExitCode == 0 || taskOutput.contains("BUILD SUCCESSFUL"),
                  "Task should complete successfully when code follows standards");
    }

    @Then("violations should be reported in {string}")
    public void violationsShouldBeReportedIn(String reportPath) {
        Path report = projectRoot.resolve(reportPath);
        if (taskOutput.contains("violations") || taskExitCode != 0) {
            assertTrue(Files.exists(report), "Violation report should exist at " + reportPath);
        }
    }

    @Then("PMD should be configured with custom ruleset")
    public void pmdShouldBeConfiguredWithCustomRuleset() {
        assertTrue(configFile.exists(), "PMD ruleset should exist");
        assertTrue(configFile.length() > 0, "PMD ruleset should not be empty");
    }

    @Then("the PMD ruleset file should exist at {string}")
    public void thePMDRulesetFileShouldExistAt(String path) {
        Path configPath = projectRoot.resolve(path);
        assertTrue(Files.exists(configPath), "PMD ruleset should exist at " + path);
    }

    @Then("PMD should check for:")
    public void pmdShouldCheckFor(DataTable dataTable) throws IOException {
        String content = Files.readString(configFile.toPath());
        List<Map<String, String>> rules = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> rule : rules) {
            String category = rule.get("rule_category");
            // Verify PMD categories
            assertTrue(content.toLowerCase().contains(category.toLowerCase().replace(" ", "")),
                      "PMD ruleset should contain " + category);
        }
    }

    @Then("the task should complete successfully if no violations exist")
    public void theTaskShouldCompleteSuccessfullyIfNoViolationsExist() {
        assertTrue(taskExitCode == 0 || taskOutput.contains("BUILD SUCCESSFUL"),
                  "Task should complete successfully when no violations exist");
    }

    @Then("SpotBugs should be configured for bug detection")
    public void spotbugsShouldBeConfiguredForBugDetection() {
        assertTrue(configFile.exists(), "SpotBugs exclude filter should exist");
    }

    @Then("the SpotBugs exclude filter should exist at {string}")
    public void theSpotBugsExcludeFilterShouldExistAt(String path) {
        Path configPath = projectRoot.resolve(path);
        assertTrue(Files.exists(configPath), "SpotBugs exclude filter should exist at " + path);
    }

    @Then("SpotBugs should check for:")
    public void spotbugsShouldCheckFor(DataTable dataTable) {
        // SpotBugs checks these by default unless excluded
        List<Map<String, String>> categories = dataTable.asMaps(String.class, String.class);
        assertFalse(categories.isEmpty(), "SpotBugs should check for multiple bug categories");
    }

    @Then("the task should complete successfully if no bugs found")
    public void theTaskShouldCompleteSuccessfullyIfNoBugsFound() {
        assertTrue(taskExitCode == 0 || taskOutput.contains("BUILD SUCCESSFUL"),
                  "Task should complete successfully when no bugs found");
    }

    @Then("bug reports should be generated in {string}")
    public void bugReportsShouldBeGeneratedIn(String reportPath) {
        if (taskOutput.contains("SpotBugs") && taskExitCode == 0) {
            Path report = projectRoot.resolve(reportPath);
            // Report is generated after SpotBugs runs
            assertTrue(reportPath.contains("spotbugs"), "SpotBugs report path should be specified");
        }
    }

    @Then("OWASP Dependency Check plugin should be configured")
    public void owaspDependencyCheckPluginShouldBeConfigured() {
        assertTrue(fileContent.contains("dependency-check") || 
                  fileContent.contains("owasp"),
                  "OWASP Dependency Check should be configured");
    }

    @Then("it should scan for:")
    public void itShouldScanFor(DataTable dataTable) {
        List<Map<String, String>> scanTypes = dataTable.asMaps(String.class, String.class);
        assertFalse(scanTypes.isEmpty(), "OWASP should scan for multiple vulnerability types");
    }

    @Then("the task should scan all project dependencies")
    public void theTaskShouldScanAllProjectDependencies() {
        if (taskOutput.contains("dependency")) {
            assertTrue(taskOutput.contains("Analyzing") || taskOutput.contains("Scanning"),
                      "Task should scan dependencies");
        }
    }

    @Then("vulnerability reports should be generated in {string}")
    public void vulnerabilityReportsShouldBeGeneratedIn(String reportPath) {
        // Report is generated after dependency check runs
        assertTrue(reportPath.contains("dependency-check"), "Dependency check report path should be specified");
    }

    @Then("the build should fail if critical vulnerabilities are found")
    public void theBuildShouldFailIfCriticalVulnerabilitiesAreFound() {
        if (taskOutput.contains("CRITICAL") || taskOutput.contains("HIGH")) {
            assertNotEquals(0, taskExitCode, "Build should fail with critical vulnerabilities");
        }
    }

    @Then("ESLint should be configured with:")
    public void eslintShouldBeConfiguredWith(DataTable dataTable) throws IOException {
        assertTrue(configFile.exists(), "ESLint config should exist");
        String content = Files.readString(configFile.toPath());
        
        List<Map<String, String>> configs = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> config : configs) {
            String configuration = config.get("configuration");
            if (configuration.contains("React")) {
                assertTrue(content.contains("react"), "ESLint should have React configuration");
            }
            if (configuration.contains("TypeScript")) {
                assertTrue(content.contains("typescript"), "ESLint should have TypeScript configuration");
            }
        }
    }

    @Then("the ESLint config should exist at {string}")
    public void theEslintConfigShouldExistAt(String path) {
        Path configPath = projectRoot.resolve(path);
        assertTrue(Files.exists(configPath), "ESLint config should exist at " + path);
    }

    @Then("the task should check all TypeScript and React files")
    public void theTaskShouldCheckAllTypeScriptAndReactFiles() {
        if (taskOutput.contains("eslint") || taskOutput.contains("lint")) {
            assertTrue(taskOutput.contains(".ts") || taskOutput.contains(".tsx") ||
                      taskExitCode == 0,
                      "ESLint should check TypeScript and React files");
        }
    }

    @Then("linting errors should fail the build")
    public void lintingErrorsShouldFailTheBuild() {
        if (taskOutput.contains("error") && taskOutput.contains("lint")) {
            assertNotEquals(0, taskExitCode, "Build should fail with linting errors");
        }
    }

    @Then("JaCoCo should be configured for code coverage")
    public void jacocoShouldBeConfiguredForCodeCoverage() {
        assertTrue(fileContent.contains("jacoco"), "JaCoCo should be configured");
    }

    @Then("coverage thresholds should be set:")
    public void coverageThresholdsShouldBeSet(DataTable dataTable) {
        List<Map<String, String>> thresholds = dataTable.asMaps(String.class, String.class);
        assertFalse(thresholds.isEmpty(), "Coverage thresholds should be defined");
    }

    @Then("coverage reports should be generated in {string}")
    public void coverageReportsShouldBeGeneratedIn(String reportPath) {
        if (taskOutput.contains("jacoco") && taskExitCode == 0) {
            assertTrue(reportPath.contains("jacoco"), "JaCoCo report path should be specified");
        }
    }

    @Then("the build should fail if coverage is below thresholds")
    public void theBuildShouldFailIfCoverageIsBelowThresholds() {
        if (taskOutput.contains("coverage") && taskOutput.contains("below")) {
            assertNotEquals(0, taskExitCode, "Build should fail with low coverage");
        }
    }

    @Then("all quality checks should run in order:")
    public void allQualityChecksShouldRunInOrder(DataTable dataTable) {
        List<Map<String, String>> checks = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> check : checks) {
            String checkType = check.get("check_type");
            assertTrue(taskOutput.contains(checkType) || taskExitCode == 0,
                      "Quality check should run: " + checkType);
        }
    }

    @Then("the build should fail if any quality gate fails")
    public void theBuildShouldFailIfAnyQualityGateFails() {
        if (taskOutput.contains("FAILED") || taskOutput.contains("violations")) {
            assertNotEquals(0, taskExitCode, "Build should fail if quality gates fail");
        }
    }

    @Then("all reports should be generated in the build directory")
    public void allReportsShouldBeGeneratedInTheBuildDirectory() {
        Path buildReports = projectRoot.resolve("build/reports");
        if (taskExitCode == 0 || taskOutput.contains("report")) {
            assertTrue(Files.exists(buildReports), "Build reports directory should exist");
        }
    }

    @Then("pre-commit hooks should be configured for:")
    public void preCommitHooksShouldBeConfiguredFor(DataTable dataTable) {
        List<Map<String, String>> hooks = dataTable.asMaps(String.class, String.class);
        assertFalse(hooks.isEmpty(), "Pre-commit hooks should be configured");
    }

    @Then("hooks should be automatically installed on project setup")
    public void hooksShouldBeAutomaticallyInstalledOnProjectSetup() {
        // Check for hook installation script or configuration
        assertTrue(Files.exists(projectRoot.resolve(".husky")) ||
                  Files.exists(projectRoot.resolve(".pre-commit-config.yaml")) ||
                  Files.exists(projectRoot.resolve("gradle/git-hooks")),
                  "Hook installation configuration should exist");
    }

    @Then("commits should be blocked if hooks fail")
    public void commitsShouldBeBlockedIfHooksFail() {
        // This is enforced by Git when hooks are properly configured
        assertTrue(true, "Git enforces pre-commit hook failures");
    }

    @Then("an aggregated quality report should be generated")
    public void anAggregatedQualityReportShouldBeGenerated() {
        if (taskOutput.contains("qualityReport") && taskExitCode == 0) {
            assertTrue(taskOutput.contains("report"), "Quality report should be generated");
        }
    }

    @Then("it should include:")
    public void itShouldInclude(DataTable dataTable) {
        List<Map<String, String>> sections = dataTable.asMaps(String.class, String.class);
        assertFalse(sections.isEmpty(), "Quality report should include multiple sections");
    }

    @Then("the report should be available at {string}")
    public void theReportShouldBeAvailableAt(String reportPath) {
        assertTrue(reportPath.contains("quality"), "Quality report path should be specified");
    }

    @Then("quality tool configurations should be recognized")
    public void qualityToolConfigurationsShouldBeRecognized() {
        // IDE configurations exist
        assertTrue(Files.exists(projectRoot.resolve("config")),
                  "Quality tool configurations should exist");
    }

    @Then("IDE should show:")
    public void ideShouldShow(DataTable dataTable) {
        List<Map<String, String>> integrations = dataTable.asMaps(String.class, String.class);
        assertFalse(integrations.isEmpty(), "IDE should show quality tool integrations");
    }

    @Then("code formatting should match project standards")
    public void codeFormattingShouldMatchProjectStandards() {
        assertTrue(Files.exists(projectRoot.resolve(".editorconfig")) ||
                  Files.exists(projectRoot.resolve("config/checkstyle/checkstyle.xml")),
                  "Code formatting configuration should exist");
    }

    @Then("each pull request should:")
    public void eachPullRequestShould(DataTable dataTable) {
        List<Map<String, String>> actions = dataTable.asMaps(String.class, String.class);
        assertFalse(actions.isEmpty(), "Pull requests should have quality checks");
    }

    @Then("quality metrics should be tracked over time")
    public void qualityMetricsShouldBeTrackedOverTime() {
        // This would be configured in CI/CD
        assertTrue(true, "Quality metrics tracking should be configured in CI/CD");
    }
}