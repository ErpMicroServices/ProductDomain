package com.erpmicroservices.productdomain.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Docker Configuration Tests")
class DockerConfigurationTest {

    private Path projectRoot;
    
    @BeforeEach
    void setUp() {
        projectRoot = Path.of(System.getProperty("user.dir"));
    }

    @Nested
    @DisplayName("API Dockerfile Configuration")
    class APIDockerfileTest {
        
        private File dockerfile;
        private String content;
        
        @BeforeEach
        void setUp() throws IOException {
            dockerfile = projectRoot.resolve("api/Dockerfile").toFile();
            if (dockerfile.exists()) {
                content = Files.readString(dockerfile.toPath());
            }
        }
        
        @Test
        @DisplayName("Should exist in API module")
        void shouldExistInAPIModule() {
            assertTrue(dockerfile.exists(), "API Dockerfile should exist");
        }
        
        @Test
        @DisplayName("Should use multi-stage build")
        void shouldUseMultiStageBuild() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            Pattern pattern = Pattern.compile("FROM .* AS \\w+");
            Matcher matcher = pattern.matcher(content);
            
            int stageCount = 0;
            while (matcher.find()) {
                stageCount++;
            }
            
            assertTrue(stageCount >= 2, "Should have at least 2 build stages");
        }
        
        @Test
        @DisplayName("Should use appropriate base images")
        void shouldUseAppropriateBaseImages() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            // Check for Gradle build image
            assertTrue(content.contains("gradle:8.5-jdk21") || content.contains("gradle:8-jdk21"),
                      "Should use Gradle 8.5 with JDK 21 for building");
            
            // Check for runtime image
            assertTrue(content.contains("eclipse-temurin:21-jre") || content.contains("openjdk:21-jre"),
                      "Should use JRE 21 for runtime");
        }
        
        @Test
        @DisplayName("Should optimize for layer caching")
        void shouldOptimizeForLayerCaching() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            // Check that dependencies are copied before source code
            int gradleFilesIndex = content.indexOf("COPY *.gradle");
            int sourceCodeIndex = content.indexOf("COPY src");
            
            assertTrue(gradleFilesIndex < sourceCodeIndex,
                      "Gradle files should be copied before source code for better caching");
        }
        
        @Test
        @DisplayName("Should run as non-root user")
        void shouldRunAsNonRootUser() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("USER ") && !content.contains("USER root"),
                      "Should specify non-root user");
        }
        
        @Test
        @DisplayName("Should expose correct port")
        void shouldExposeCorrectPort() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("EXPOSE 8080"),
                      "Should expose port 8080 for Spring Boot");
        }
        
        @Test
        @DisplayName("Should have health check")
        void shouldHaveHealthCheck() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("HEALTHCHECK"),
                      "Should define HEALTHCHECK instruction");
        }
    }

    @Nested
    @DisplayName("UI Dockerfile Configuration")
    class UIDockerfileTest {
        
        private File dockerfile;
        private String content;
        
        @BeforeEach
        void setUp() throws IOException {
            dockerfile = projectRoot.resolve("ui-components/Dockerfile").toFile();
            if (dockerfile.exists()) {
                content = Files.readString(dockerfile.toPath());
            }
        }
        
        @Test
        @DisplayName("Should exist in UI module")
        void shouldExistInUIModule() {
            assertTrue(dockerfile.exists(), "UI Dockerfile should exist");
        }
        
        @Test
        @DisplayName("Should use Node for building")
        void shouldUseNodeForBuilding() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("node:20") || content.contains("node:18"),
                      "Should use Node.js for building");
        }
        
        @Test
        @DisplayName("Should use nginx for serving")
        void shouldUseNginxForServing() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("nginx:alpine"),
                      "Should use nginx:alpine for serving static files");
        }
        
        @Test
        @DisplayName("Should copy built files to nginx")
        void shouldCopyBuiltFilesToNginx() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("COPY --from=") && content.contains("/usr/share/nginx/html"),
                      "Should copy built files from builder to nginx");
        }
        
        @Test
        @DisplayName("Should include nginx configuration")
        void shouldIncludeNginxConfiguration() {
            assertNotNull(content, "Dockerfile content should be loaded");
            
            assertTrue(content.contains("COPY nginx.conf") || content.contains("COPY default.conf"),
                      "Should copy custom nginx configuration");
        }
    }

    @Nested
    @DisplayName("Docker Compose Development Configuration")
    class DockerComposeDevTest {
        
        private File composeFile;
        private Map<String, Object> composeConfig;
        
        @BeforeEach
        void setUp() throws IOException {
            composeFile = projectRoot.resolve("docker-compose.dev.yml").toFile();
            if (composeFile.exists()) {
                Yaml yaml = new Yaml();
                try (FileInputStream inputStream = new FileInputStream(composeFile)) {
                    composeConfig = yaml.load(inputStream);
                }
            }
        }
        
        @Test
        @DisplayName("Should exist in project root")
        void shouldExistInProjectRoot() {
            assertTrue(composeFile.exists(), "docker-compose.dev.yml should exist");
        }
        
        @Test
        @DisplayName("Should define all required services")
        void shouldDefineAllRequiredServices() {
            assertNotNull(composeConfig, "Compose config should be loaded");
            
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            assertNotNull(services, "Services should be defined");
            
            assertTrue(services.containsKey("postgres"), "Should have postgres service");
            assertTrue(services.containsKey("redis"), "Should have redis service");
            assertTrue(services.containsKey("api"), "Should have api service");
            assertTrue(services.containsKey("ui"), "Should have ui service");
        }
        
        @Test
        @DisplayName("Should configure PostgreSQL correctly")
        void shouldConfigurePostgreSQLCorrectly() {
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            Map<String, Object> postgres = (Map<String, Object>) services.get("postgres");
            
            assertEquals("postgres:16-alpine", postgres.get("image"));
            
            Map<String, String> environment = (Map<String, String>) postgres.get("environment");
            assertTrue(environment.containsKey("POSTGRES_DB"));
            assertTrue(environment.containsKey("POSTGRES_USER"));
            assertTrue(environment.containsKey("POSTGRES_PASSWORD"));
            
            List<String> volumes = (List<String>) postgres.get("volumes");
            assertTrue(volumes.stream().anyMatch(v -> v.contains("postgres_data")));
        }
        
        @Test
        @DisplayName("Should configure Redis correctly")
        void shouldConfigureRedisCorrectly() {
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            Map<String, Object> redis = (Map<String, Object>) services.get("redis");
            
            assertEquals("redis:7-alpine", redis.get("image"));
            
            List<String> volumes = (List<String>) redis.get("volumes");
            assertTrue(volumes.stream().anyMatch(v -> v.contains("redis_data")));
        }
        
        @Test
        @DisplayName("Should have health checks for all services")
        void shouldHaveHealthChecksForAllServices() {
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            for (String serviceName : List.of("postgres", "redis", "api", "ui")) {
                Map<String, Object> service = (Map<String, Object>) services.get(serviceName);
                assertTrue(service.containsKey("healthcheck"),
                          serviceName + " should have health check");
            }
        }
        
        @Test
        @DisplayName("Should configure networks properly")
        void shouldConfigureNetworksProperly() {
            assertTrue(composeConfig.containsKey("networks"), "Should define networks");
            
            Map<String, Object> networks = (Map<String, Object>) composeConfig.get("networks");
            assertTrue(networks.containsKey("backend"), "Should have backend network");
            assertTrue(networks.containsKey("frontend"), "Should have frontend network");
        }
        
        @Test
        @DisplayName("Should configure volumes for development")
        void shouldConfigureVolumesForDevelopment() {
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            // Check API has source volume for hot reload
            Map<String, Object> api = (Map<String, Object>) services.get("api");
            List<String> apiVolumes = (List<String>) api.get("volumes");
            assertTrue(apiVolumes.stream().anyMatch(v -> v.contains("./api/src")),
                      "API should mount source for hot reload");
            
            // Check UI has source volume for hot reload
            Map<String, Object> ui = (Map<String, Object>) services.get("ui");
            List<String> uiVolumes = (List<String>) ui.get("volumes");
            assertTrue(uiVolumes.stream().anyMatch(v -> v.contains("./ui-components/src")),
                      "UI should mount source for hot reload");
        }
    }

    @Nested
    @DisplayName("Docker Compose Production Configuration")
    class DockerComposeProdTest {
        
        private File composeFile;
        private Map<String, Object> composeConfig;
        
        @BeforeEach
        void setUp() throws IOException {
            composeFile = projectRoot.resolve("docker-compose.prod.yml").toFile();
            if (composeFile.exists()) {
                Yaml yaml = new Yaml();
                try (FileInputStream inputStream = new FileInputStream(composeFile)) {
                    composeConfig = yaml.load(inputStream);
                }
            }
        }
        
        @Test
        @DisplayName("Should exist in project root")
        void shouldExistInProjectRoot() {
            assertTrue(composeFile.exists(), "docker-compose.prod.yml should exist");
        }
        
        @Test
        @DisplayName("Should NOT have source volume mounts")
        void shouldNotHaveSourceVolumeMounts() {
            if (composeConfig == null) return;
            
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                Map<String, Object> service = (Map<String, Object>) entry.getValue();
                if (service.containsKey("volumes")) {
                    List<String> volumes = (List<String>) service.get("volumes");
                    for (String volume : volumes) {
                        assertFalse(volume.startsWith("./src") || volume.startsWith("./app"),
                                   entry.getKey() + " should not mount source code in production");
                    }
                }
            }
        }
        
        @Test
        @DisplayName("Should have restart policies")
        void shouldHaveRestartPolicies() {
            if (composeConfig == null) return;
            
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                Map<String, Object> service = (Map<String, Object>) entry.getValue();
                assertTrue(service.containsKey("restart"),
                          entry.getKey() + " should have restart policy");
                assertEquals("unless-stopped", service.get("restart"));
            }
        }
        
        @Test
        @DisplayName("Should have resource limits")
        void shouldHaveResourceLimits() {
            if (composeConfig == null) return;
            
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            for (String serviceName : List.of("api", "ui")) {
                Map<String, Object> service = (Map<String, Object>) services.get(serviceName);
                assertTrue(service.containsKey("deploy"),
                          serviceName + " should have deploy configuration");
                
                Map<String, Object> deploy = (Map<String, Object>) service.get("deploy");
                assertTrue(deploy.containsKey("resources"),
                          serviceName + " should have resource limits");
            }
        }
        
        @Test
        @DisplayName("Should have security configurations")
        void shouldHaveSecurityConfigurations() {
            if (composeConfig == null) return;
            
            Map<String, Object> services = (Map<String, Object>) composeConfig.get("services");
            
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                Map<String, Object> service = (Map<String, Object>) entry.getValue();
                if (service.containsKey("security_opt")) {
                    List<String> securityOpts = (List<String>) service.get("security_opt");
                    assertTrue(securityOpts.contains("no-new-privileges:true"),
                              entry.getKey() + " should have no-new-privileges");
                }
            }
        }
    }

    @Nested
    @DisplayName("Docker Security Configuration")
    class DockerSecurityTest {
        
        @Test
        @DisplayName("Should have .dockerignore file")
        void shouldHaveDockerignoreFile() {
            File dockerignore = projectRoot.resolve(".dockerignore").toFile();
            assertTrue(dockerignore.exists(), ".dockerignore should exist");
        }
        
        @Test
        @DisplayName("Dockerignore should exclude sensitive files")
        void dockerignoreShouldExcludeSensitiveFiles() throws IOException {
            File dockerignore = projectRoot.resolve(".dockerignore").toFile();
            if (dockerignore.exists()) {
                String content = Files.readString(dockerignore.toPath());
                
                assertTrue(content.contains(".env"), "Should exclude .env files");
                assertTrue(content.contains(".git"), "Should exclude .git directory");
                assertTrue(content.contains("*.log"), "Should exclude log files");
                assertTrue(content.contains("node_modules"), "Should exclude node_modules");
                assertTrue(content.contains(".gradle"), "Should exclude .gradle directory");
            }
        }
        
        @Test
        @DisplayName("Should have environment example file")
        void shouldHaveEnvironmentExampleFile() {
            File envExample = projectRoot.resolve(".env.example").toFile();
            assertTrue(envExample.exists(), ".env.example should exist");
        }
        
        @Test
        @DisplayName("Environment example should contain all required variables")
        void environmentExampleShouldContainAllRequiredVariables() throws IOException {
            File envExample = projectRoot.resolve(".env.example").toFile();
            if (envExample.exists()) {
                String content = Files.readString(envExample.toPath());
                
                // Database variables
                assertTrue(content.contains("POSTGRES_DB="), "Should have POSTGRES_DB");
                assertTrue(content.contains("POSTGRES_USER="), "Should have POSTGRES_USER");
                assertTrue(content.contains("POSTGRES_PASSWORD="), "Should have POSTGRES_PASSWORD");
                
                // Redis variables
                assertTrue(content.contains("REDIS_PASSWORD="), "Should have REDIS_PASSWORD");
                
                // Application variables
                assertTrue(content.contains("SPRING_PROFILES_ACTIVE="), "Should have SPRING_PROFILES_ACTIVE");
                assertTrue(content.contains("API_BASE_URL="), "Should have API_BASE_URL");
            }
        }
    }

    @Nested
    @DisplayName("Build Optimization Tests")
    class BuildOptimizationTest {
        
        @Test
        @DisplayName("API Dockerfile should use layer caching effectively")
        void apiDockerfileShouldUseLayerCachingEffectively() throws IOException {
            File dockerfile = projectRoot.resolve("api/Dockerfile").toFile();
            if (dockerfile.exists()) {
                String content = Files.readString(dockerfile.toPath());
                
                // Check order of COPY commands for better caching
                int gradleIndex = content.indexOf("COPY build.gradle");
                int sourceIndex = content.indexOf("COPY src");
                
                assertTrue(gradleIndex > 0, "Should copy gradle files");
                assertTrue(sourceIndex > gradleIndex, 
                          "Source should be copied after gradle files for better caching");
            }
        }
        
        @Test
        @DisplayName("UI Dockerfile should optimize for production build")
        void uiDockerfileShouldOptimizeForProductionBuild() throws IOException {
            File dockerfile = projectRoot.resolve("ui-components/Dockerfile").toFile();
            if (dockerfile.exists()) {
                String content = Files.readString(dockerfile.toPath());
                
                // Check for production build optimizations
                assertTrue(content.contains("npm ci") || content.contains("npm install --production"),
                          "Should use npm ci or production install");
                assertTrue(content.contains("npm run build"),
                          "Should have production build command");
            }
        }
        
        @Test
        @DisplayName("Should use Alpine-based images where possible")
        void shouldUseAlpineBasedImagesWherePossible() throws IOException {
            // Check API Dockerfile
            File apiDockerfile = projectRoot.resolve("api/Dockerfile").toFile();
            if (apiDockerfile.exists()) {
                String apiContent = Files.readString(apiDockerfile.toPath());
                assertTrue(apiContent.contains("-alpine") || apiContent.contains("eclipse-temurin"),
                          "API should use Alpine or minimal base image");
            }
            
            // Check UI Dockerfile
            File uiDockerfile = projectRoot.resolve("ui-components/Dockerfile").toFile();
            if (uiDockerfile.exists()) {
                String uiContent = Files.readString(uiDockerfile.toPath());
                assertTrue(uiContent.contains("nginx:alpine"),
                          "UI should use nginx:alpine");
            }
        }
    }
}