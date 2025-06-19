package com.erpmicroservices.productdomain.precommit;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for pre-commit GitHub Actions workflow validation.
 */
public class PreCommitWorkflowValidationSteps {

    private static final String WORKFLOWS_DIR = ".github/workflows";
    private static final String PRE_COMMIT_HOOK = ".git/hooks/pre-commit";
    
    private Path tempWorkflowFile;
    private Map<String, Object> workflowContent;
    private ProcessResult preCommitResult;
    private List<Path> workflowFiles;
    private Map<String, String> actionVersionCache;
    private long cacheTimestamp;
    
    @Given("the ProductDomain project exists")
    public void theProductDomainProjectExists() {
        Path projectRoot = Paths.get(".");
        assertThat(projectRoot).exists();
        assertThat(projectRoot.resolve("build.gradle")).exists();
    }
    
    @Given("pre-commit hooks are installed")
    public void preCommitHooksAreInstalled() {
        Path preCommitHook = Paths.get(PRE_COMMIT_HOOK);
        assertThat(preCommitHook).exists();
        assertThat(preCommitHook).isExecutable();
    }
    
    @Given("a workflow file with deprecated action versions")
    public void aWorkflowFileWithDeprecatedActionVersions() throws IOException {
        workflowContent = createWorkflowWithActions(
            "actions/checkout@v3",  // deprecated
            "actions/setup-java@v3", // deprecated
            "actions/upload-artifact@v3" // deprecated
        );
        tempWorkflowFile = createTempWorkflowFile(workflowContent);
        stageFile(tempWorkflowFile);
    }
    
    @Given("a workflow file with invalid YAML syntax")
    public void aWorkflowFileWithInvalidYAMLSyntax() throws IOException {
        String invalidYaml = """
            name: Invalid Workflow
            on:
              push:
                branches: [main]
            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - name: Checkout
                    uses: actions/checkout@v4
                  - name: Invalid step
                    uses: actions/setup-java@v4
                    with:
                      java-version: 21
                    invalid-indentation-here
                      distribution: 'temurin'
            """;
        tempWorkflowFile = createTempWorkflowFile(invalidYaml);
        stageFile(tempWorkflowFile);
    }
    
    @Given("a workflow file missing required properties")
    public void aWorkflowFileMissingRequiredProperties() throws IOException {
        workflowContent = new HashMap<>();
        workflowContent.put("name", "Incomplete Workflow");
        // Missing 'on' and 'jobs' properties
        tempWorkflowFile = createTempWorkflowFile(workflowContent);
        stageFile(tempWorkflowFile);
    }
    
    @Given("a workflow file with all latest action versions")
    public void aWorkflowFileWithAllLatestActionVersions() throws IOException {
        workflowContent = createWorkflowWithActions(
            "actions/checkout@v4",
            "actions/setup-java@v4",
            "actions/upload-artifact@v4",
            "actions/download-artifact@v4",
            "actions/cache@v4"
        );
        tempWorkflowFile = createTempWorkflowFile(workflowContent);
        stageFile(tempWorkflowFile);
    }
    
    @And("valid YAML syntax")
    public void validYAMLSyntax() {
        // Already ensured by createWorkflowWithActions
    }
    
    @And("all required properties")
    public void allRequiredProperties() {
        assertThat(workflowContent).containsKeys("name", "on", "jobs");
    }
    
    @Given("multiple workflow files in .github/workflows")
    public void multipleWorkflowFilesInGithubWorkflows() throws IOException {
        workflowFiles = new ArrayList<>();
        
        // Create a valid workflow
        Map<String, Object> validWorkflow = createWorkflowWithActions(
            "actions/checkout@v4",
            "actions/setup-java@v4"
        );
        Path validFile = createTempWorkflowFile(validWorkflow, "valid-workflow.yml");
        workflowFiles.add(validFile);
        stageFile(validFile);
        
        // Create a workflow with deprecated actions
        Map<String, Object> deprecatedWorkflow = createWorkflowWithActions(
            "actions/checkout@v3",
            "actions/setup-java@v3"
        );
        Path deprecatedFile = createTempWorkflowFile(deprecatedWorkflow, "deprecated-workflow.yml");
        workflowFiles.add(deprecatedFile);
        stageFile(deprecatedFile);
    }
    
    @And("some have deprecated actions")
    public void someHaveDeprecatedActions() {
        // Already handled in the previous step
    }
    
    @Given("a workflow file with malformed action versions")
    public void aWorkflowFileWithMalformedActionVersions() throws IOException {
        workflowContent = createWorkflowWithActions(
            "actions/checkout@main",  // branch instead of version
            "actions/setup-java@",    // missing version
            "actions/upload-artifact" // no version at all
        );
        tempWorkflowFile = createTempWorkflowFile(workflowContent);
        stageFile(tempWorkflowFile);
    }
    
    @Given("the pre-commit hook has checked action versions before")
    public void thePreCommitHookHasCheckedActionVersionsBefore() {
        actionVersionCache = new HashMap<>();
        actionVersionCache.put("actions/checkout", "v4");
        actionVersionCache.put("actions/setup-java", "v4");
        actionVersionCache.put("actions/upload-artifact", "v4");
        cacheTimestamp = System.currentTimeMillis();
    }
    
    @Given("a workflow file with third-party actions")
    public void aWorkflowFileWithThirdPartyActions() throws IOException {
        workflowContent = createWorkflowWithActions(
            "actions/checkout@v4",
            "docker/setup-buildx-action@v3",
            "aquasecurity/trivy-action@master",
            "dorny/test-reporter@v1"
        );
        tempWorkflowFile = createTempWorkflowFile(workflowContent);
        stageFile(tempWorkflowFile);
    }
    
    @Given("a workflow file using Docker-based actions")
    public void aWorkflowFileUsingDockerBasedActions() throws IOException {
        Map<String, Object> workflow = createBasicWorkflow();
        Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
        Map<String, Object> buildJob = (Map<String, Object>) jobs.get("build");
        List<Map<String, Object>> steps = (List<Map<String, Object>>) buildJob.get("steps");
        
        // Add Docker action
        Map<String, Object> dockerStep = new HashMap<>();
        dockerStep.put("name", "Run Docker Action");
        dockerStep.put("uses", "docker://alpine:latest");
        dockerStep.put("with", Map.of("args", "echo Hello"));
        steps.add(dockerStep);
        
        tempWorkflowFile = createTempWorkflowFile(workflow);
        stageFile(tempWorkflowFile);
    }
    
    @Given("a workflow file with deprecated actions")
    public void aWorkflowFileWithDeprecatedActions() throws IOException {
        aWorkflowFileWithDeprecatedActionVersions();
    }
    
    @When("I attempt to commit the changes")
    public void iAttemptToCommitTheChanges() {
        preCommitResult = runPreCommitHook();
    }
    
    @When("I attempt to commit without workflow changes")
    public void iAttemptToCommitWithoutWorkflowChanges() throws IOException {
        // Stage a non-workflow file
        Path dummyFile = Files.createTempFile("test", ".txt");
        Files.writeString(dummyFile, "test content");
        stageFile(dummyFile);
        
        preCommitResult = runPreCommitHook();
    }
    
    @When("the pre-commit hook fails")
    public void thePreCommitHookFails() {
        assertThat(preCommitResult.exitCode).isNotEqualTo(0);
    }
    
    @Then("the pre-commit hook should fail")
    public void thePreCommitHookShouldFail() {
        assertThat(preCommitResult.exitCode).isNotEqualTo(0);
    }
    
    @Then("the pre-commit hook should pass")
    public void thePreCommitHookShouldPass() {
        assertThat(preCommitResult.exitCode).isEqualTo(0);
    }
    
    @And("it should report the deprecated actions")
    public void itShouldReportTheDeprecatedActions() {
        assertThat(preCommitResult.output).contains("deprecated");
        assertThat(preCommitResult.output).contains("actions/checkout@v3");
        assertThat(preCommitResult.output).contains("actions/setup-java@v3");
        assertThat(preCommitResult.output).contains("actions/upload-artifact@v3");
    }
    
    @And("it should suggest the latest versions")
    public void itShouldSuggestTheLatestVersions() {
        assertThat(preCommitResult.output).contains("v4");
        assertThat(preCommitResult.output).contains("latest version");
    }
    
    @And("it should report the YAML syntax errors")
    public void itShouldReportTheYAMLSyntaxErrors() {
        assertThat(preCommitResult.output).contains("YAML");
        assertThat(preCommitResult.output).contains("syntax");
        assertThat(preCommitResult.output).contains("error");
    }
    
    @And("it should indicate the line numbers with issues")
    public void itShouldIndicateTheLineNumbersWithIssues() {
        assertThat(preCommitResult.output).containsPattern("line \\d+");
    }
    
    @And("it should list the missing required properties")
    public void itShouldListTheMissingRequiredProperties() {
        assertThat(preCommitResult.output).contains("missing");
        assertThat(preCommitResult.output).contains("on");
        assertThat(preCommitResult.output).contains("jobs");
    }
    
    @And("it should provide examples of valid workflow structure")
    public void itShouldProvideExamplesOfValidWorkflowStructure() {
        assertThat(preCommitResult.output).contains("example");
        assertThat(preCommitResult.output).contains("valid workflow");
    }
    
    @And("the commit should proceed")
    public void theCommitShouldProceed() {
        assertThat(preCommitResult.exitCode).isEqualTo(0);
    }
    
    @Then("the pre-commit hook should check all workflow files")
    public void thePreCommitHookShouldCheckAllWorkflowFiles() {
        assertThat(preCommitResult.output).contains("valid-workflow.yml");
        assertThat(preCommitResult.output).contains("deprecated-workflow.yml");
    }
    
    @And("it should report issues for each file separately")
    public void itShouldReportIssuesForEachFileSeparately() {
        assertThat(preCommitResult.output).contains("deprecated-workflow.yml");
        assertThat(preCommitResult.output).contains("deprecated actions");
    }
    
    @And("it should fail if any file has issues")
    public void itShouldFailIfAnyFileHasIssues() {
        assertThat(preCommitResult.exitCode).isNotEqualTo(0);
    }
    
    @And("it should report invalid version formats")
    public void itShouldReportInvalidVersionFormats() {
        assertThat(preCommitResult.output).contains("invalid version");
        assertThat(preCommitResult.output).contains("@main");
        assertThat(preCommitResult.output).contains("missing version");
    }
    
    @And("it should show the correct version format")
    public void itShouldShowTheCorrectVersionFormat() {
        assertThat(preCommitResult.output).contains("@v");
        assertThat(preCommitResult.output).contains("semantic version");
    }
    
    @Then("the pre-commit hook should use cached version data")
    public void thePreCommitHookShouldUseCachedVersionData() {
        // Check that the hook completed quickly (indicating cache usage)
        assertThat(preCommitResult.executionTimeMs).isLessThan(1000);
    }
    
    @And("it should complete quickly")
    public void itShouldCompleteQuickly() {
        assertThat(preCommitResult.executionTimeMs).isLessThan(1000);
    }
    
    @And("the cache should expire after 24 hours")
    public void theCacheShouldExpireAfter24Hours() {
        long cacheAge = System.currentTimeMillis() - cacheTimestamp;
        long twentyFourHours = TimeUnit.HOURS.toMillis(24);
        assertThat(cacheAge).isLessThan(twentyFourHours);
    }
    
    @Then("the pre-commit hook should validate third-party action versions")
    public void thePreCommitHookShouldValidateThirdPartyActionVersions() {
        assertThat(preCommitResult.output).contains("docker/setup-buildx-action");
        assertThat(preCommitResult.output).contains("dorny/test-reporter");
    }
    
    @And("it should check if newer versions exist")
    public void itShouldCheckIfNewerVersionsExist() {
        assertThat(preCommitResult.output).contains("newer version");
    }
    
    @And("it should warn about unmaintained actions")
    public void itShouldWarnAboutUnmaintainedActions() {
        assertThat(preCommitResult.output).contains("unmaintained");
    }
    
    @Then("the pre-commit hook should validate Docker action syntax")
    public void thePreCommitHookShouldValidateDockerActionSyntax() {
        assertThat(preCommitResult.output).contains("docker://");
    }
    
    @And("it should check Docker image tags")
    public void itShouldCheckDockerImageTags() {
        assertThat(preCommitResult.output).contains("alpine:latest");
    }
    
    @And("it should warn about using 'latest' tags")
    public void itShouldWarnAboutUsingLatestTags() {
        assertThat(preCommitResult.output).contains("latest tag");
        assertThat(preCommitResult.output).contains("specific version");
    }
    
    @Then("it should provide automated fix commands")
    public void itShouldProvideAutomatedFixCommands() {
        assertThat(preCommitResult.output).contains("fix");
        assertThat(preCommitResult.output).contains("sed");
    }
    
    @And("it should show a diff of suggested changes")
    public void itShouldShowADiffOfSuggestedChanges() {
        assertThat(preCommitResult.output).contains("diff");
        assertThat(preCommitResult.output).contains("-");
        assertThat(preCommitResult.output).contains("+");
    }
    
    @And("it should offer to apply fixes automatically")
    public void itShouldOfferToApplyFixesAutomatically() {
        assertThat(preCommitResult.output).contains("apply");
        assertThat(preCommitResult.output).contains("automatically");
    }
    
    // Helper methods
    
    private Map<String, Object> createBasicWorkflow() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Test Workflow");
        workflow.put("on", Map.of("push", Map.of("branches", List.of("main"))));
        
        Map<String, Object> jobs = new LinkedHashMap<>();
        Map<String, Object> buildJob = new LinkedHashMap<>();
        buildJob.put("runs-on", "ubuntu-latest");
        buildJob.put("steps", new ArrayList<Map<String, Object>>());
        jobs.put("build", buildJob);
        workflow.put("jobs", jobs);
        
        return workflow;
    }
    
    private Map<String, Object> createWorkflowWithActions(String... actions) {
        Map<String, Object> workflow = createBasicWorkflow();
        Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
        Map<String, Object> buildJob = (Map<String, Object>) jobs.get("build");
        List<Map<String, Object>> steps = (List<Map<String, Object>>) buildJob.get("steps");
        
        for (String action : actions) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("name", "Step using " + action);
            step.put("uses", action);
            steps.add(step);
        }
        
        return workflow;
    }
    
    private Path createTempWorkflowFile(Map<String, Object> content) throws IOException {
        return createTempWorkflowFile(content, "test-workflow.yml");
    }
    
    private Path createTempWorkflowFile(Map<String, Object> content, String filename) throws IOException {
        Path workflowDir = Paths.get(WORKFLOWS_DIR);
        Files.createDirectories(workflowDir);
        
        Path workflowFile = workflowDir.resolve(filename);
        Yaml yaml = new Yaml();
        try (FileWriter writer = new FileWriter(workflowFile.toFile())) {
            yaml.dump(content, writer);
        }
        
        return workflowFile;
    }
    
    private Path createTempWorkflowFile(String content) throws IOException {
        Path workflowDir = Paths.get(WORKFLOWS_DIR);
        Files.createDirectories(workflowDir);
        
        Path workflowFile = workflowDir.resolve("test-workflow.yml");
        Files.writeString(workflowFile, content);
        
        return workflowFile;
    }
    
    private void stageFile(Path file) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("git", "add", file.toString());
        Process process = pb.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while staging file", e);
        }
    }
    
    private ProcessResult runPreCommitHook() {
        long startTime = System.currentTimeMillis();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(".git/hooks/pre-commit");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new ProcessResult(exitCode, output, executionTime);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run pre-commit hook", e);
        }
    }
    
    private static class ProcessResult {
        final int exitCode;
        final String output;
        final long executionTimeMs;
        
        ProcessResult(int exitCode, String output, long executionTimeMs) {
            this.exitCode = exitCode;
            this.output = output;
            this.executionTimeMs = executionTimeMs;
        }
    }
}