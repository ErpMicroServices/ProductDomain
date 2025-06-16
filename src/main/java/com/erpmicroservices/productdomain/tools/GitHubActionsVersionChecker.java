package com.erpmicroservices.productdomain.tools;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates GitHub Actions versions in workflow files.
 */
public class GitHubActionsVersionChecker {

    private static final Pattern ACTION_PATTERN = Pattern.compile("^([^/]+/[^@]+)@(.+)$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+(\\.\\d+)?(\\.\\d+)?$");
    private static final Pattern SHA_PATTERN = Pattern.compile("^[a-f0-9]{40}$");
    private static final Pattern DOCKER_ACTION_PATTERN = Pattern.compile("^docker://(.+)$");
    
    private final Map<String, String> knownActionsLatestVersions;
    private final Map<String, Date> deprecationDates;
    private final Map<String, String> versionCache;
    private long cacheExpiration = 24 * 60 * 60 * 1000; // 24 hours
    private long lastCacheUpdate;
    
    public GitHubActionsVersionChecker() {
        this.knownActionsLatestVersions = initializeKnownActions();
        this.deprecationDates = initializeDeprecationDates();
        this.versionCache = new ConcurrentHashMap<>();
        this.lastCacheUpdate = System.currentTimeMillis();
    }
    
    private Map<String, String> initializeKnownActions() {
        Map<String, String> actions = new HashMap<>();
        // GitHub official actions
        actions.put("actions/checkout", "v4");
        actions.put("actions/setup-java", "v4");
        actions.put("actions/setup-node", "v4");
        actions.put("actions/setup-python", "v4");
        actions.put("actions/upload-artifact", "v4");
        actions.put("actions/download-artifact", "v4");
        actions.put("actions/cache", "v4");
        actions.put("actions/github-script", "v7");
        actions.put("actions/setup-go", "v5");
        
        // Common third-party actions
        actions.put("docker/setup-buildx-action", "v3");
        actions.put("docker/build-push-action", "v5");
        actions.put("docker/login-action", "v3");
        actions.put("docker/setup-qemu-action", "v3");
        actions.put("docker/metadata-action", "v5");
        actions.put("aquasecurity/trivy-action", "master");
        actions.put("dorny/test-reporter", "v1");
        actions.put("gradle/gradle-build-action", "v2");
        actions.put("softprops/action-gh-release", "v1");
        actions.put("sigstore/cosign-installer", "v3");
        actions.put("anchore/sbom-action", "v0");
        actions.put("anchore/scan-action", "v3");
        actions.put("github/codeql-action", "v3");
        
        return actions;
    }
    
    private Map<String, Date> initializeDeprecationDates() {
        Map<String, Date> dates = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        
        // v3 actions deprecated on September 1, 2023
        cal.set(2023, Calendar.SEPTEMBER, 1);
        dates.put("v3", cal.getTime());
        
        // v2 actions deprecated earlier
        cal.set(2023, Calendar.JANUARY, 1);
        dates.put("v2", cal.getTime());
        
        return dates;
    }
    
    public ValidationResult validateActionVersion(String actionRef) {
        if (actionRef == null || actionRef.isEmpty()) {
            return ValidationResult.invalid("Action reference is empty");
        }
        
        // Check for Docker actions
        if (actionRef.startsWith("docker://")) {
            return validateDockerAction(actionRef);
        }
        
        // Parse action reference
        Matcher matcher = ACTION_PATTERN.matcher(actionRef);
        if (!matcher.matches()) {
            if (!actionRef.contains("@")) {
                return ValidationResult.invalid("Missing version: " + actionRef + " should include @version");
            }
            return ValidationResult.invalid("Invalid action format: " + actionRef);
        }
        
        String actionName = matcher.group(1);
        String version = matcher.group(2);
        
        // Check version format
        if (SHA_PATTERN.matcher(version).matches()) {
            return ValidationResult.warning("Using commit SHA. Consider using a version tag for stability: " + actionRef);
        }
        
        if ("main".equals(version) || "master".equals(version) || "develop".equals(version)) {
            return ValidationResult.invalid("Using branch reference '" + version + "'. Use a version tag instead (e.g., @v4)");
        }
        
        if (!VERSION_PATTERN.matcher(version).matches()) {
            return ValidationResult.invalid("Invalid version format '" + version + "'. Use semantic versioning (e.g., v1, v1.0, v1.0.0)");
        }
        
        // Check if action is known
        String latestVersion = knownActionsLatestVersions.get(actionName);
        if (latestVersion != null) {
            // Check if version is deprecated
            if (isVersionDeprecated(version)) {
                return ValidationResult.deprecated(
                    "Action " + actionRef + " uses deprecated version " + version + 
                    ". Latest version is " + latestVersion,
                    latestVersion
                );
            }
            
            // Check if using latest version
            if (!version.equals(latestVersion)) {
                return ValidationResult.warning(
                    "Action " + actionRef + " is not using the latest version. " +
                    "Latest version is " + latestVersion
                );
            }
        }
        
        return ValidationResult.valid();
    }
    
    public ValidationResult validateDockerAction(String dockerRef) {
        Matcher matcher = DOCKER_ACTION_PATTERN.matcher(dockerRef);
        if (!matcher.matches()) {
            return ValidationResult.invalid("Invalid Docker action format: " + dockerRef);
        }
        
        String imageRef = matcher.group(1);
        if (imageRef.endsWith(":latest")) {
            return ValidationResult.invalid(
                "Using ':latest' tag is not recommended. Use a specific version tag for reproducibility"
            );
        }
        
        if (!imageRef.contains(":")) {
            return ValidationResult.invalid(
                "Docker image should include a specific tag (e.g., alpine:3.18)"
            );
        }
        
        return ValidationResult.valid();
    }
    
    public WorkflowValidationResult validateWorkflowFile(Path workflowFile) {
        WorkflowValidationResult result = new WorkflowValidationResult(workflowFile.getFileName().toString());
        
        try {
            // Parse YAML
            Yaml yaml = new Yaml();
            Map<String, Object> workflow;
            
            try (FileInputStream fis = new FileInputStream(workflowFile.toFile())) {
                workflow = yaml.load(fis);
            } catch (YAMLException e) {
                result.addIssue(new Issue(
                    IssueType.YAML_SYNTAX,
                    "Invalid YAML syntax: " + e.getMessage(),
                    getLineNumber(e.getMessage())
                ));
                return result;
            }
            
            if (workflow == null) {
                result.addIssue(new Issue(IssueType.YAML_SYNTAX, "Empty workflow file", 1));
                return result;
            }
            
            // Check required properties
            if (!workflow.containsKey("name")) {
                result.addIssue(new Issue(IssueType.MISSING_PROPERTY, "Missing required property 'name'", 1));
            }
            if (!workflow.containsKey("on")) {
                result.addIssue(new Issue(IssueType.MISSING_PROPERTY, "Missing required property 'on'", 1));
            }
            if (!workflow.containsKey("jobs")) {
                result.addIssue(new Issue(IssueType.MISSING_PROPERTY, "Missing required property 'jobs'", 1));
            }
            
            // Validate all actions in the workflow
            Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
            if (jobs != null) {
                validateJobs(jobs, result);
            }
            
        } catch (IOException e) {
            result.addIssue(new Issue(IssueType.FILE_ERROR, "Failed to read workflow file: " + e.getMessage(), 0));
        }
        
        return result;
    }
    
    private void validateJobs(Map<String, Object> jobs, WorkflowValidationResult result) {
        for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
            String jobName = jobEntry.getKey();
            Map<String, Object> job = (Map<String, Object>) jobEntry.getValue();
            
            if (job == null) continue;
            
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            if (steps != null) {
                int stepNumber = 0;
                for (Map<String, Object> step : steps) {
                    stepNumber++;
                    String uses = (String) step.get("uses");
                    if (uses != null && !uses.isEmpty()) {
                        ValidationResult validation = validateActionVersion(uses);
                        if (!validation.isValid()) {
                            result.addIssue(new Issue(
                                validation.isDeprecated() ? IssueType.DEPRECATED_VERSION : IssueType.INVALID_VERSION,
                                validation.getMessage(),
                                stepNumber,
                                uses,
                                validation.getSuggestedVersion()
                            ));
                        }
                    }
                }
            }
        }
    }
    
    private boolean isVersionDeprecated(String version) {
        return deprecationDates.containsKey(version);
    }
    
    private int getLineNumber(String errorMessage) {
        // Try to extract line number from YAML error message
        Pattern linePattern = Pattern.compile("line (\\d+)");
        Matcher matcher = linePattern.matcher(errorMessage);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    public boolean isKnownAction(String actionName) {
        return knownActionsLatestVersions.containsKey(actionName);
    }
    
    public String getLatestVersion(String actionName) {
        // Check cache first
        if (!isCacheExpired() && versionCache.containsKey(actionName)) {
            return versionCache.get(actionName);
        }
        
        // Get from known actions
        String version = knownActionsLatestVersions.getOrDefault(actionName, "unknown");
        versionCache.put(actionName, version);
        
        return version;
    }
    
    public Map<String, Date> getDeprecationDates() {
        return new HashMap<>(deprecationDates);
    }
    
    public void setCacheExpiration(long millis) {
        this.cacheExpiration = millis;
    }
    
    public boolean isCacheExpired() {
        return System.currentTimeMillis() - lastCacheUpdate > cacheExpiration;
    }
    
    public String generateFixCommand(String filename, String oldAction, String newAction) {
        return String.format("sed -i '' 's|%s|%s|g' %s", 
            oldAction.replace("/", "\\/"), 
            newAction.replace("/", "\\/"), 
            filename);
    }
    
    public String generateDiff(String filename, List<ActionUpdate> updates) {
        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(filename).append("\n");
        diff.append("+++ b/").append(filename).append("\n");
        
        for (ActionUpdate update : updates) {
            diff.append("@@ -").append(update.line).append(",1 +").append(update.line).append(",1 @@\n");
            diff.append("-        uses: ").append(update.oldAction).append("\n");
            diff.append("+        uses: ").append(update.newAction).append("\n");
        }
        
        return diff.toString();
    }
    
    public String generateBatchFix(Map<String, List<ActionUpdate>> fileUpdates) {
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("# Batch fix script for GitHub Actions version updates\n\n");
        
        for (Map.Entry<String, List<ActionUpdate>> entry : fileUpdates.entrySet()) {
            String filename = entry.getKey();
            script.append("echo 'Updating ").append(filename).append("...'\n");
            
            for (ActionUpdate update : entry.getValue()) {
                script.append(generateFixCommand(filename, update.oldAction, update.newAction)).append("\n");
            }
            script.append("\n");
        }
        
        script.append("echo 'All fixes applied!'\n");
        return script.toString();
    }
    
    // Result classes
    
    public static class ValidationResult {
        private final boolean valid;
        private final boolean deprecated;
        private final String message;
        private final String suggestedVersion;
        
        private ValidationResult(boolean valid, boolean deprecated, String message, String suggestedVersion) {
            this.valid = valid;
            this.deprecated = deprecated;
            this.message = message;
            this.suggestedVersion = suggestedVersion;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, false, null, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, false, message, null);
        }
        
        public static ValidationResult deprecated(String message, String suggestedVersion) {
            return new ValidationResult(false, true, message, suggestedVersion);
        }
        
        public static ValidationResult warning(String message) {
            return new ValidationResult(true, false, message, null);
        }
        
        public boolean isValid() { return valid; }
        public boolean isDeprecated() { return deprecated; }
        public String getMessage() { return message; }
        public String getSuggestedVersion() { return suggestedVersion; }
    }
    
    public static class WorkflowValidationResult {
        private final String filename;
        private final List<Issue> issues = new ArrayList<>();
        
        public WorkflowValidationResult(String filename) {
            this.filename = filename;
        }
        
        public void addIssue(Issue issue) {
            issues.add(issue);
        }
        
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
        
        public List<Issue> getIssues() {
            return new ArrayList<>(issues);
        }
        
        public String getFilename() {
            return filename;
        }
    }
    
    public static class Issue {
        private final IssueType type;
        private final String message;
        private final int line;
        private final String action;
        private final String suggestedFix;
        
        public Issue(IssueType type, String message, int line) {
            this(type, message, line, null, null);
        }
        
        public Issue(IssueType type, String message, int line, String action, String suggestedFix) {
            this.type = type;
            this.message = message;
            this.line = line;
            this.action = action;
            this.suggestedFix = suggestedFix;
        }
        
        public IssueType getType() { return type; }
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public String getAction() { return action; }
        public String getSuggestedFix() { return suggestedFix; }
    }
    
    public enum IssueType {
        DEPRECATED_VERSION,
        MISSING_VERSION,
        INVALID_VERSION,
        INVALID_FORMAT,
        YAML_SYNTAX,
        MISSING_PROPERTY,
        FILE_ERROR
    }
    
    public static class ActionUpdate {
        public final String oldAction;
        public final String newAction;
        public final int line;
        
        public ActionUpdate(String oldAction, String newAction, int line) {
            this.oldAction = oldAction;
            this.newAction = newAction;
            this.line = line;
        }
    }
}