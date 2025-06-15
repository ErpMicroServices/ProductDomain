package com.erpmicroservices.productdomain.cicd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHub Actions workflow validation.
 */
@DisplayName("GitHub Actions Workflow Validation")
class WorkflowValidationTest {

    private Yaml yaml;
    private DumperOptions dumperOptions;

    @BeforeEach
    void setUp() {
        dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        yaml = new Yaml(dumperOptions);
    }

    @Nested
    @DisplayName("Workflow Structure Validation")
    class WorkflowStructureValidation {

        @Test
        @DisplayName("Should have required top-level properties")
        void shouldHaveRequiredTopLevelProperties() {
            Map<String, Object> workflow = createBasicWorkflow();
            
            assertThat(workflow).containsKeys("name", "on", "jobs");
            assertThat(workflow.get("name")).isInstanceOf(String.class);
            assertThat(workflow.get("on")).isNotNull();
            assertThat(workflow.get("jobs")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should validate job structure")
        void shouldValidateJobStructure() {
            Map<String, Object> job = createBasicJob();
            
            assertThat(job).containsKey("runs-on");
            assertThat(job).containsKey("steps");
            assertThat(job.get("steps")).isInstanceOf(List.class);
            assertThat((List<?>) job.get("steps")).isNotEmpty();
        }

        @Test
        @DisplayName("Should validate step structure")
        void shouldValidateStepStructure() {
            Map<String, Object> step = createBasicStep();
            
            assertThat(step).containsAnyOf("uses", "run");
            if (step.containsKey("uses")) {
                assertThat(step.get("uses")).isInstanceOf(String.class);
            }
            if (step.containsKey("run")) {
                assertThat(step.get("run")).isInstanceOf(String.class);
            }
        }
    }

    @Nested
    @DisplayName("CI Workflow Validation")
    class CIWorkflowValidation {

        @Test
        @DisplayName("Should create valid main CI workflow")
        void shouldCreateValidMainCIWorkflow() {
            Map<String, Object> workflow = createMainCIWorkflow();
            
            assertThat(workflow.get("name")).isEqualTo("Main CI");
            
            Map<String, Object> on = (Map<String, Object>) workflow.get("on");
            assertThat(on).containsKey("pull_request");
            assertThat(on).containsKey("push");
            
            Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
            assertThat(jobs).containsKeys("quality-checks", "test", "security-scan");
        }

        @Test
        @DisplayName("Should include code quality checks")
        void shouldIncludeCodeQualityChecks() {
            Map<String, Object> job = createQualityCheckJob();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            boolean hasCheckstyle = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "checkstyle"));
            boolean hasPMD = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "pmd"));
            boolean hasSpotBugs = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "spotbugs"));
            
            assertThat(hasCheckstyle).isTrue();
            assertThat(hasPMD).isTrue();
            assertThat(hasSpotBugs).isTrue();
        }

        @Test
        @DisplayName("Should include test execution with coverage")
        void shouldIncludeTestExecutionWithCoverage() {
            Map<String, Object> job = createTestJob();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            boolean hasTest = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "test"));
            boolean hasCoverage = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "jacoco"));
            
            assertThat(hasTest).isTrue();
            assertThat(hasCoverage).isTrue();
        }

        @Test
        @DisplayName("Should include security scanning")
        void shouldIncludeSecurityScanning() {
            Map<String, Object> job = createSecurityScanJob();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            boolean hasOWASP = steps.stream()
                .anyMatch(step -> stepContainsTool(step, "dependencyCheck"));
            
            assertThat(hasOWASP).isTrue();
        }
    }

    @Nested
    @DisplayName("Docker Workflow Validation")
    class DockerWorkflowValidation {

        @Test
        @DisplayName("Should create valid Docker build workflow")
        void shouldCreateValidDockerBuildWorkflow() {
            Map<String, Object> workflow = createDockerWorkflow();
            
            assertThat(workflow.get("name")).isEqualTo("Docker Build and Push");
            
            Map<String, Object> on = (Map<String, Object>) workflow.get("on");
            Map<String, Object> push = (Map<String, Object>) on.get("push");
            List<String> branches = (List<String>) push.get("branches");
            assertThat(branches).contains("main");
            
            Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
            assertThat(jobs).containsKeys("build-api", "build-ui", "scan-images");
        }

        @Test
        @DisplayName("Should use Docker buildx")
        void shouldUseDockerBuildx() {
            Map<String, Object> job = createDockerBuildJob();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            boolean hasSetupBuildx = steps.stream()
                .anyMatch(step -> {
                    String uses = (String) step.get("uses");
                    return uses != null && uses.contains("docker/setup-buildx-action");
                });
            
            assertThat(hasSetupBuildx).isTrue();
        }

        @Test
        @DisplayName("Should push to registry with proper tags")
        void shouldPushToRegistryWithProperTags() {
            Map<String, Object> job = createDockerBuildJob();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            Optional<Map<String, Object>> buildStep = steps.stream()
                .filter(step -> {
                    String uses = (String) step.get("uses");
                    return uses != null && uses.contains("docker/build-push-action");
                })
                .findFirst();
            
            assertThat(buildStep).isPresent();
            Map<String, Object> with = (Map<String, Object>) buildStep.get().get("with");
            assertThat(with).containsKey("tags");
            assertThat(with.get("push")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Deployment Workflow Validation")
    class DeploymentWorkflowValidation {

        @Test
        @DisplayName("Should create valid deployment workflow")
        void shouldCreateValidDeploymentWorkflow() {
            Map<String, Object> workflow = createDeploymentWorkflow();
            
            assertThat(workflow.get("name")).isEqualTo("Deploy");
            
            Map<String, Object> on = (Map<String, Object>) workflow.get("on");
            assertThat(on).containsKey("workflow_dispatch");
            
            Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
            assertThat(jobs).containsKeys("deploy-dev", "deploy-staging", "deploy-prod");
        }

        @Test
        @DisplayName("Should include environment validation")
        void shouldIncludeEnvironmentValidation() {
            Map<String, Object> job = createDeploymentJob("staging");
            
            assertThat(job).containsKey("environment");
            assertThat(job.get("environment")).isEqualTo("staging");
            
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            boolean hasValidation = steps.stream()
                .anyMatch(step -> {
                    String name = (String) step.get("name");
                    return name != null && name.contains("Validate environment");
                });
            
            assertThat(hasValidation).isTrue();
        }

        @Test
        @DisplayName("Should include rollback capability")
        void shouldIncludeRollbackCapability() {
            Map<String, Object> job = createDeploymentJob("production");
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            
            boolean hasRollback = steps.stream()
                .anyMatch(step -> {
                    String name = (String) step.get("name");
                    return name != null && name.toLowerCase().contains("rollback");
                });
            
            assertThat(hasRollback).isTrue();
        }
    }

    @Nested
    @DisplayName("Matrix Build Validation")
    class MatrixBuildValidation {

        @Test
        @DisplayName("Should create valid matrix strategy")
        void shouldCreateValidMatrixStrategy() {
            Map<String, Object> job = createMatrixJob();
            Map<String, Object> strategy = (Map<String, Object>) job.get("strategy");
            
            assertThat(strategy).containsKey("matrix");
            Map<String, Object> matrix = (Map<String, Object>) strategy.get("matrix");
            
            assertThat(matrix).containsKeys("java-version", "os");
            assertThat((List<String>) matrix.get("java-version"))
                .containsExactly("17", "21");
            assertThat((List<String>) matrix.get("os"))
                .containsExactly("ubuntu-latest", "windows-latest", "macos-latest");
        }

        @Test
        @DisplayName("Should use matrix variables in job")
        void shouldUseMatrixVariablesInJob() {
            Map<String, Object> job = createMatrixJob();
            
            assertThat(job.get("runs-on")).isEqualTo("${{ matrix.os }}");
            
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");
            Optional<Map<String, Object>> javaSetup = steps.stream()
                .filter(step -> {
                    String uses = (String) step.get("uses");
                    return uses != null && uses.contains("setup-java");
                })
                .findFirst();
            
            assertThat(javaSetup).isPresent();
            Map<String, Object> with = (Map<String, Object>) javaSetup.get().get("with");
            assertThat(with.get("java-version")).isEqualTo("${{ matrix.java-version }}");
        }
    }

    @Nested
    @DisplayName("Artifact Management Validation")
    class ArtifactManagementValidation {

        @Test
        @DisplayName("Should upload artifacts with retention")
        void shouldUploadArtifactsWithRetention() {
            Map<String, Object> step = createArtifactUploadStep();
            
            assertThat(step.get("uses")).isEqualTo("actions/upload-artifact@v4");
            Map<String, Object> with = (Map<String, Object>) step.get("with");
            
            assertThat(with).containsKeys("name", "path", "retention-days");
            assertThat(with.get("retention-days")).isEqualTo(30);
        }

        @Test
        @DisplayName("Should download artifacts when needed")
        void shouldDownloadArtifactsWhenNeeded() {
            Map<String, Object> step = createArtifactDownloadStep();
            
            assertThat(step.get("uses")).isEqualTo("actions/download-artifact@v4");
            Map<String, Object> with = (Map<String, Object>) step.get("with");
            
            assertThat(with).containsKey("name");
        }
    }

    @Test
    @DisplayName("Should generate valid YAML from workflow map")
    void shouldGenerateValidYAMLFromWorkflowMap(@TempDir Path tempDir) throws IOException {
        Map<String, Object> workflow = createMainCIWorkflow();
        
        Path workflowFile = tempDir.resolve("test-workflow.yml");
        try (FileWriter writer = new FileWriter(workflowFile.toFile())) {
            yaml.dump(workflow, writer);
        }
        
        // Verify the file was created and can be loaded
        assertThat(workflowFile).exists();
        
        Map<String, Object> loaded = yaml.load(Files.readString(workflowFile));
        assertThat(loaded).isEqualTo(workflow);
    }

    // Helper methods for creating workflow components
    
    private Map<String, Object> createBasicWorkflow() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Test Workflow");
        workflow.put("on", Collections.singletonMap("push", null));
        workflow.put("jobs", Collections.singletonMap("test", createBasicJob()));
        return workflow;
    }
    
    private Map<String, Object> createBasicJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        job.put("steps", Arrays.asList(createBasicStep()));
        return job;
    }
    
    private Map<String, Object> createBasicStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Checkout code");
        step.put("uses", "actions/checkout@v4");
        return step;
    }
    
    private Map<String, Object> createMainCIWorkflow() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Main CI");
        
        Map<String, Object> on = new LinkedHashMap<>();
        on.put("pull_request", null);
        on.put("push", Collections.singletonMap("branches", Arrays.asList("main", "develop")));
        workflow.put("on", on);
        
        Map<String, Object> jobs = new LinkedHashMap<>();
        jobs.put("quality-checks", createQualityCheckJob());
        jobs.put("test", createTestJob());
        jobs.put("security-scan", createSecurityScanJob());
        workflow.put("jobs", jobs);
        
        return workflow;
    }
    
    private Map<String, Object> createQualityCheckJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        steps.add(createSetupJavaStep());
        steps.add(createRunStep("Run Checkstyle", "./gradlew checkstyleMain checkstyleTest"));
        steps.add(createRunStep("Run PMD", "./gradlew pmdMain pmdTest"));
        steps.add(createRunStep("Run SpotBugs", "./gradlew spotbugsMain spotbugsTest"));
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createTestJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        steps.add(createSetupJavaStep());
        steps.add(createRunStep("Run tests", "./gradlew test"));
        steps.add(createRunStep("Generate coverage report", "./gradlew jacocoTestReport"));
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createSecurityScanJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        steps.add(createSetupJavaStep());
        steps.add(createRunStep("Run OWASP Dependency Check", "./gradlew dependencyCheckAnalyze"));
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createDockerWorkflow() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Docker Build and Push");
        
        Map<String, Object> on = new LinkedHashMap<>();
        Map<String, Object> push = new LinkedHashMap<>();
        push.put("branches", Arrays.asList("main"));
        push.put("tags", Arrays.asList("v*"));
        on.put("push", push);
        workflow.put("on", on);
        
        Map<String, Object> jobs = new LinkedHashMap<>();
        jobs.put("build-api", createDockerBuildJob());
        jobs.put("build-ui", createDockerBuildJob());
        jobs.put("scan-images", createImageScanJob());
        workflow.put("jobs", jobs);
        
        return workflow;
    }
    
    private Map<String, Object> createDockerBuildJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        steps.add(createDockerSetupStep());
        steps.add(createDockerBuildxStep());
        steps.add(createDockerLoginStep());
        steps.add(createDockerBuildPushStep());
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createDeploymentWorkflow() {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("name", "Deploy");
        
        Map<String, Object> on = new LinkedHashMap<>();
        Map<String, Object> workflowDispatch = new LinkedHashMap<>();
        Map<String, Object> inputs = new LinkedHashMap<>();
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("description", "Deployment environment");
        environment.put("required", true);
        environment.put("type", "choice");
        environment.put("options", Arrays.asList("dev", "staging", "production"));
        inputs.put("environment", environment);
        workflowDispatch.put("inputs", inputs);
        on.put("workflow_dispatch", workflowDispatch);
        workflow.put("on", on);
        
        Map<String, Object> jobs = new LinkedHashMap<>();
        jobs.put("deploy-dev", createDeploymentJob("dev"));
        jobs.put("deploy-staging", createDeploymentJob("staging"));
        jobs.put("deploy-prod", createDeploymentJob("production"));
        workflow.put("jobs", jobs);
        
        return workflow;
    }
    
    private Map<String, Object> createDeploymentJob(String environment) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        job.put("environment", environment);
        
        if (!environment.equals("dev")) {
            job.put("needs", Arrays.asList("deploy-" + 
                (environment.equals("staging") ? "dev" : "staging")));
        }
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        steps.add(createRunStep("Validate environment", 
            "echo 'Validating " + environment + " environment'"));
        steps.add(createRunStep("Deploy to " + environment, 
            "echo 'Deploying to " + environment + "'"));
        steps.add(createRunStep("Run smoke tests", 
            "echo 'Running smoke tests'"));
        
        if (environment.equals("production")) {
            steps.add(createRunStep("Rollback on failure", 
                "echo 'Ready to rollback if needed'"));
        }
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createMatrixJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "${{ matrix.os }}");
        
        Map<String, Object> strategy = new LinkedHashMap<>();
        Map<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("java-version", Arrays.asList("17", "21"));
        matrix.put("os", Arrays.asList("ubuntu-latest", "windows-latest", "macos-latest"));
        strategy.put("matrix", matrix);
        strategy.put("fail-fast", false);
        job.put("strategy", strategy);
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createCheckoutStep());
        
        Map<String, Object> setupJava = new LinkedHashMap<>();
        setupJava.put("uses", "actions/setup-java@v4");
        Map<String, Object> javaWith = new LinkedHashMap<>();
        javaWith.put("distribution", "temurin");
        javaWith.put("java-version", "${{ matrix.java-version }}");
        setupJava.put("with", javaWith);
        steps.add(setupJava);
        
        steps.add(createRunStep("Run tests", "./gradlew test"));
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createImageScanJob() {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("runs-on", "ubuntu-latest");
        job.put("needs", Arrays.asList("build-api", "build-ui"));
        
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(createRunStep("Scan images", "echo 'Scanning Docker images for vulnerabilities'"));
        
        job.put("steps", steps);
        return job;
    }
    
    private Map<String, Object> createCheckoutStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Checkout code");
        step.put("uses", "actions/checkout@v4");
        return step;
    }
    
    private Map<String, Object> createSetupJavaStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Set up JDK");
        step.put("uses", "actions/setup-java@v4");
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("distribution", "temurin");
        with.put("java-version", "21");
        step.put("with", with);
        return step;
    }
    
    private Map<String, Object> createRunStep(String name, String command) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", name);
        step.put("run", command);
        return step;
    }
    
    private Map<String, Object> createDockerSetupStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Set up Docker");
        step.put("uses", "docker/setup-qemu-action@v3");
        return step;
    }
    
    private Map<String, Object> createDockerBuildxStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Set up Docker Buildx");
        step.put("uses", "docker/setup-buildx-action@v3");
        return step;
    }
    
    private Map<String, Object> createDockerLoginStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Login to Docker Hub");
        step.put("uses", "docker/login-action@v3");
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("username", "${{ secrets.DOCKER_USERNAME }}");
        with.put("password", "${{ secrets.DOCKER_PASSWORD }}");
        step.put("with", with);
        return step;
    }
    
    private Map<String, Object> createDockerBuildPushStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Build and push Docker image");
        step.put("uses", "docker/build-push-action@v5");
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("context", ".");
        with.put("push", true);
        with.put("tags", "user/app:latest");
        step.put("with", with);
        return step;
    }
    
    private Map<String, Object> createArtifactUploadStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Upload artifacts");
        step.put("uses", "actions/upload-artifact@v4");
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("name", "build-artifacts");
        with.put("path", "build/libs/");
        with.put("retention-days", 30);
        step.put("with", with);
        return step;
    }
    
    private Map<String, Object> createArtifactDownloadStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Download artifacts");
        step.put("uses", "actions/download-artifact@v4");
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("name", "build-artifacts");
        step.put("with", with);
        return step;
    }
    
    private boolean stepContainsTool(Map<String, Object> step, String tool) {
        String run = (String) step.get("run");
        String name = (String) step.get("name");
        return (run != null && run.toLowerCase().contains(tool.toLowerCase())) ||
               (name != null && name.toLowerCase().contains(tool.toLowerCase()));
    }
}