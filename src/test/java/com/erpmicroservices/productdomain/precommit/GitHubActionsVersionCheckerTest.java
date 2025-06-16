package com.erpmicroservices.productdomain.precommit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHub Actions version checking functionality.
 */
@DisplayName("GitHub Actions Version Checker")
class GitHubActionsVersionCheckerTest {

    private GitHubActionsVersionChecker versionChecker;
    private Yaml yaml;

    @BeforeEach
    void setUp() {
        versionChecker = new GitHubActionsVersionChecker();
        yaml = new Yaml();
    }

    @Nested
    @DisplayName("Action Version Detection")
    class ActionVersionDetection {

        @Test
        @DisplayName("Should detect deprecated v3 actions")
        void shouldDetectDeprecatedV3Actions() {
            String action = "actions/checkout@v3";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("deprecated");
            assertThat(result.getSuggestedVersion()).isEqualTo("v4");
        }

        @Test
        @DisplayName("Should accept latest v4 actions")
        void shouldAcceptLatestV4Actions() {
            String action = "actions/checkout@v4";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should detect missing version")
        void shouldDetectMissingVersion() {
            String action = "actions/checkout";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("missing version");
        }

        @Test
        @DisplayName("Should detect branch references instead of versions")
        void shouldDetectBranchReferences() {
            String action = "actions/checkout@main";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("branch reference");
            assertThat(result.getMessage()).contains("use version tag");
        }

        @Test
        @DisplayName("Should handle commit SHA references")
        void shouldHandleCommitSHAReferences() {
            String action = "actions/checkout@8e5e7e5ab8b370d6c329ec480221332ada57f0ab";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).contains("commit SHA");
            assertThat(result.getMessage()).contains("consider using version tag");
        }
    }

    @Nested
    @DisplayName("Known Actions Database")
    class KnownActionsDatabase {

        @Test
        @DisplayName("Should know all common GitHub actions")
        void shouldKnowAllCommonGitHubActions() {
            List<String> commonActions = Arrays.asList(
                "actions/checkout",
                "actions/setup-java",
                "actions/setup-node",
                "actions/setup-python",
                "actions/upload-artifact",
                "actions/download-artifact",
                "actions/cache",
                "actions/github-script"
            );

            for (String action : commonActions) {
                assertThat(versionChecker.isKnownAction(action)).isTrue();
                assertThat(versionChecker.getLatestVersion(action)).isEqualTo("v4");
            }
        }

        @Test
        @DisplayName("Should track action deprecation timeline")
        void shouldTrackActionDeprecationTimeline() {
            Map<String, Date> deprecationDates = versionChecker.getDeprecationDates();
            
            assertThat(deprecationDates).containsKey("v3");
            assertThat(deprecationDates.get("v3")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Third-Party Actions")
    class ThirdPartyActions {

        @Test
        @DisplayName("Should validate third-party action format")
        void shouldValidateThirdPartyActionFormat() {
            String action = "docker/setup-buildx-action@v3";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should detect unmaintained actions")
        void shouldDetectUnmaintainedActions() {
            // Mock an action that hasn't been updated in over a year
            String action = "old-org/unmaintained-action@v1";
            
            ValidationResult result = versionChecker.checkActionMaintenance(action);
            
            assertThat(result.getMessage()).contains("unmaintained");
            assertThat(result.getMessage()).contains("consider alternative");
        }

        @Test
        @DisplayName("Should validate Docker Hub actions")
        void shouldValidateDockerHubActions() {
            String action = "docker/build-push-action@v5";
            
            ValidationResult result = versionChecker.validateActionVersion(action);
            
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Docker Actions")
    class DockerActions {

        @Test
        @DisplayName("Should warn about latest tag")
        void shouldWarnAboutLatestTag() {
            String action = "docker://alpine:latest";
            
            ValidationResult result = versionChecker.validateDockerAction(action);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("latest tag");
            assertThat(result.getMessage()).contains("specific version");
        }

        @Test
        @DisplayName("Should accept specific Docker tags")
        void shouldAcceptSpecificDockerTags() {
            String action = "docker://alpine:3.18";
            
            ValidationResult result = versionChecker.validateDockerAction(action);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should validate Docker registry URLs")
        void shouldValidateDockerRegistryURLs() {
            String action = "docker://ghcr.io/owner/image:v1.0.0";
            
            ValidationResult result = versionChecker.validateDockerAction(action);
            
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Version Format Validation")
    class VersionFormatValidation {

        @Test
        @DisplayName("Should accept semantic versions")
        void shouldAcceptSemanticVersions() {
            List<String> validVersions = Arrays.asList(
                "v1", "v2", "v3", "v4",
                "v1.0", "v2.1", "v3.14",
                "v1.0.0", "v2.1.3", "v3.14.159"
            );

            for (String version : validVersions) {
                String action = "actions/checkout@" + version;
                ValidationResult result = versionChecker.validateActionVersion(action);
                assertThat(result.getMessage()).doesNotContain("invalid format");
            }
        }

        @Test
        @DisplayName("Should reject invalid version formats")
        void shouldRejectInvalidVersionFormats() {
            List<String> invalidVersions = Arrays.asList(
                "1.0.0",  // missing 'v' prefix
                "version-1",
                "latest",
                "v1.0.0-beta" // pre-release versions
            );

            for (String version : invalidVersions) {
                String action = "actions/checkout@" + version;
                ValidationResult result = versionChecker.validateActionVersion(action);
                assertThat(result.isValid()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Workflow File Validation")
    class WorkflowFileValidation {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should validate all actions in workflow")
        void shouldValidateAllActionsInWorkflow() throws IOException {
            Map<String, Object> workflow = createWorkflowWithActions(
                "actions/checkout@v3",
                "actions/setup-java@v4",
                "actions/upload-artifact@v3"
            );

            Path workflowFile = createWorkflowFile(workflow);
            
            WorkflowValidationResult result = versionChecker.validateWorkflowFile(workflowFile);
            
            assertThat(result.hasIssues()).isTrue();
            assertThat(result.getIssues()).hasSize(2); // v3 actions
            assertThat(result.getIssues())
                .extracting(Issue::getAction)
                .containsExactlyInAnyOrder(
                    "actions/checkout@v3",
                    "actions/upload-artifact@v3"
                );
        }

        @Test
        @DisplayName("Should handle invalid YAML gracefully")
        void shouldHandleInvalidYAMLGracefully() throws IOException {
            Path workflowFile = tempDir.resolve("invalid.yml");
            Files.writeString(workflowFile, "invalid:\n  yaml:\n    - content\n  missing: bracket");
            
            WorkflowValidationResult result = versionChecker.validateWorkflowFile(workflowFile);
            
            assertThat(result.hasIssues()).isTrue();
            assertThat(result.getIssues()).hasSize(1);
            assertThat(result.getIssues().get(0).getType()).isEqualTo(IssueType.YAML_SYNTAX);
        }

        @Test
        @DisplayName("Should check required workflow properties")
        void shouldCheckRequiredWorkflowProperties() throws IOException {
            Map<String, Object> workflow = new HashMap<>();
            workflow.put("name", "Incomplete Workflow");
            // Missing 'on' and 'jobs'
            
            Path workflowFile = createWorkflowFile(workflow);
            
            WorkflowValidationResult result = versionChecker.validateWorkflowFile(workflowFile);
            
            assertThat(result.hasIssues()).isTrue();
            assertThat(result.getIssues())
                .extracting(Issue::getType)
                .contains(IssueType.MISSING_PROPERTY);
        }

        private Path createWorkflowFile(Map<String, Object> content) throws IOException {
            Path workflowFile = tempDir.resolve("workflow.yml");
            try (FileWriter writer = new FileWriter(workflowFile.toFile())) {
                yaml.dump(content, writer);
            }
            return workflowFile;
        }
    }

    @Nested
    @DisplayName("Version Cache")
    class VersionCache {

        @Test
        @DisplayName("Should cache version lookups")
        void shouldCacheVersionLookups() {
            String action = "actions/checkout";
            
            // First lookup
            long start1 = System.currentTimeMillis();
            String version1 = versionChecker.getLatestVersion(action);
            long time1 = System.currentTimeMillis() - start1;
            
            // Second lookup (should be cached)
            long start2 = System.currentTimeMillis();
            String version2 = versionChecker.getLatestVersion(action);
            long time2 = System.currentTimeMillis() - start2;
            
            assertThat(version1).isEqualTo(version2);
            assertThat(time2).isLessThan(time1);
        }

        @Test
        @DisplayName("Should expire cache after 24 hours")
        void shouldExpireCacheAfter24Hours() {
            versionChecker.setCacheExpiration(1); // 1 millisecond for testing
            
            String action = "actions/checkout";
            String version1 = versionChecker.getLatestVersion(action);
            
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Should fetch again after expiration
            String version2 = versionChecker.getLatestVersion(action);
            
            assertThat(version1).isEqualTo(version2);
            assertThat(versionChecker.isCacheExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fix Suggestions")
    class FixSuggestions {

        @Test
        @DisplayName("Should generate fix commands")
        void shouldGenerateFixCommands() {
            String filename = "workflow.yml";
            String oldAction = "actions/checkout@v3";
            String newAction = "actions/checkout@v4";
            
            String fixCommand = versionChecker.generateFixCommand(filename, oldAction, newAction);
            
            assertThat(fixCommand).contains("sed");
            assertThat(fixCommand).contains(oldAction);
            assertThat(fixCommand).contains(newAction);
            assertThat(fixCommand).contains(filename);
        }

        @Test
        @DisplayName("Should generate diff for changes")
        void shouldGenerateDiffForChanges() {
            List<ActionUpdate> updates = Arrays.asList(
                new ActionUpdate("actions/checkout@v3", "actions/checkout@v4", 10),
                new ActionUpdate("actions/setup-java@v3", "actions/setup-java@v4", 15)
            );
            
            String diff = versionChecker.generateDiff("workflow.yml", updates);
            
            assertThat(diff).contains("---");
            assertThat(diff).contains("+++");
            assertThat(diff).contains("-actions/checkout@v3");
            assertThat(diff).contains("+actions/checkout@v4");
        }

        @Test
        @DisplayName("Should offer batch fix for multiple files")
        void shouldOfferBatchFixForMultipleFiles() {
            Map<String, List<ActionUpdate>> fileUpdates = new HashMap<>();
            fileUpdates.put("ci.yml", Arrays.asList(
                new ActionUpdate("actions/checkout@v3", "actions/checkout@v4", 10)
            ));
            fileUpdates.put("deploy.yml", Arrays.asList(
                new ActionUpdate("actions/setup-java@v3", "actions/setup-java@v4", 20)
            ));
            
            String batchFix = versionChecker.generateBatchFix(fileUpdates);
            
            assertThat(batchFix).contains("ci.yml");
            assertThat(batchFix).contains("deploy.yml");
            assertThat(batchFix).contains("Apply all fixes");
        }
    }

    // Helper methods
    
    private Map<String, Object> createWorkflowWithActions(String... actions) {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Test Workflow");
        workflow.put("on", Map.of("push", Map.of("branches", List.of("main"))));
        
        Map<String, Object> jobs = new LinkedHashMap<>();
        Map<String, Object> buildJob = new LinkedHashMap<>();
        buildJob.put("runs-on", "ubuntu-latest");
        
        List<Map<String, Object>> steps = new ArrayList<>();
        for (String action : actions) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("name", "Step using " + action);
            step.put("uses", action);
            steps.add(step);
        }
        buildJob.put("steps", steps);
        jobs.put("build", buildJob);
        workflow.put("jobs", jobs);
        
        return workflow;
    }

    // Mock classes for testing
    
    static class GitHubActionsVersionChecker {
        private final Map<String, String> knownActions = new HashMap<>();
        private final Map<String, String> versionCache = new HashMap<>();
        private final Map<String, Date> deprecationDates = new HashMap<>();
        private long cacheExpiration = 24 * 60 * 60 * 1000; // 24 hours
        private long lastCacheUpdate = System.currentTimeMillis();
        
        public GitHubActionsVersionChecker() {
            // Initialize known actions
            Arrays.asList(
                "actions/checkout",
                "actions/setup-java",
                "actions/setup-node",
                "actions/setup-python",
                "actions/upload-artifact",
                "actions/download-artifact",
                "actions/cache",
                "actions/github-script"
            ).forEach(action -> knownActions.put(action, "v4"));
            
            // Set deprecation dates
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.SEPTEMBER, 1);
            deprecationDates.put("v3", cal.getTime());
        }
        
        public ValidationResult validateActionVersion(String action) {
            // Implementation would go here
            return new ValidationResult();
        }
        
        public boolean isKnownAction(String action) {
            return knownActions.containsKey(action);
        }
        
        public String getLatestVersion(String action) {
            return knownActions.getOrDefault(action, "unknown");
        }
        
        public Map<String, Date> getDeprecationDates() {
            return deprecationDates;
        }
        
        public ValidationResult checkActionMaintenance(String action) {
            return new ValidationResult();
        }
        
        public ValidationResult validateDockerAction(String action) {
            return new ValidationResult();
        }
        
        public WorkflowValidationResult validateWorkflowFile(Path file) {
            return new WorkflowValidationResult();
        }
        
        public void setCacheExpiration(long millis) {
            this.cacheExpiration = millis;
        }
        
        public boolean isCacheExpired() {
            return System.currentTimeMillis() - lastCacheUpdate > cacheExpiration;
        }
        
        public String generateFixCommand(String filename, String oldAction, String newAction) {
            return String.format("sed -i 's|%s|%s|g' %s", oldAction, newAction, filename);
        }
        
        public String generateDiff(String filename, List<ActionUpdate> updates) {
            StringBuilder diff = new StringBuilder();
            diff.append("--- a/").append(filename).append("\n");
            diff.append("+++ b/").append(filename).append("\n");
            for (ActionUpdate update : updates) {
                diff.append("@@ -").append(update.line).append(" +").append(update.line).append(" @@\n");
                diff.append("-        uses: ").append(update.oldAction).append("\n");
                diff.append("+        uses: ").append(update.newAction).append("\n");
            }
            return diff.toString();
        }
        
        public String generateBatchFix(Map<String, List<ActionUpdate>> fileUpdates) {
            return "Batch fix script for multiple files";
        }
    }
    
    static class ValidationResult {
        private boolean valid = true;
        private String message;
        private String suggestedVersion;
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getSuggestedVersion() { return suggestedVersion; }
    }
    
    static class WorkflowValidationResult {
        private List<Issue> issues = new ArrayList<>();
        
        public boolean hasIssues() { return !issues.isEmpty(); }
        public List<Issue> getIssues() { return issues; }
    }
    
    static class Issue {
        private String action;
        private IssueType type;
        
        public String getAction() { return action; }
        public IssueType getType() { return type; }
    }
    
    enum IssueType {
        DEPRECATED_VERSION,
        MISSING_VERSION,
        INVALID_FORMAT,
        YAML_SYNTAX,
        MISSING_PROPERTY
    }
    
    static class ActionUpdate {
        final String oldAction;
        final String newAction;
        final int line;
        
        ActionUpdate(String oldAction, String newAction, int line) {
            this.oldAction = oldAction;
            this.newAction = newAction;
            this.line = line;
        }
    }
}