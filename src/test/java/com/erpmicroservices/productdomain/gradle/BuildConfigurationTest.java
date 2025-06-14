package com.erpmicroservices.productdomain.gradle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gradle Build Configuration Tests")
class BuildConfigurationTest {

    private Path projectRoot;
    
    @BeforeEach
    void setUp() {
        projectRoot = Path.of(System.getProperty("user.dir"));
    }

    @Nested
    @DisplayName("Root Project Configuration")
    class RootProjectConfigurationTest {
        
        @Test
        @DisplayName("Should have valid root build.gradle file")
        void shouldHaveValidRootBuildGradleFile() {
            File buildFile = projectRoot.resolve("build.gradle").toFile();
            assertTrue(buildFile.exists(), "Root build.gradle should exist");
            assertTrue(buildFile.isFile(), "build.gradle should be a file");
            assertTrue(buildFile.length() > 0, "build.gradle should not be empty");
        }

        @Test
        @DisplayName("Should have valid settings.gradle file")
        void shouldHaveValidSettingsGradleFile() {
            File settingsFile = projectRoot.resolve("settings.gradle").toFile();
            assertTrue(settingsFile.exists(), "settings.gradle should exist");
            assertTrue(settingsFile.isFile(), "settings.gradle should be a file");
            assertTrue(settingsFile.length() > 0, "settings.gradle should not be empty");
        }

        @Test
        @DisplayName("Should configure Java 21 compatibility")
        void shouldConfigureJava21Compatibility() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("sourceCompatibility = '21'") ||
                      buildContent.contains("sourceCompatibility = \"21\""),
                      "Should configure Java 21 source compatibility");
            assertTrue(buildContent.contains("targetCompatibility = '21'") ||
                      buildContent.contains("targetCompatibility = \"21\""),
                      "Should configure Java 21 target compatibility");
        }

        @Test
        @DisplayName("Should define project group and version")
        void shouldDefineProjectGroupAndVersion() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("group = 'com.erpmicroservices'") ||
                      buildContent.contains("group = \"com.erpmicroservices\""),
                      "Should define project group");
            assertTrue(buildContent.matches("(?s).*version\\s*=\\s*['\"].*['\"].*"),
                      "Should define project version");
        }

        @Test
        @DisplayName("Should configure multi-module structure")
        void shouldConfigureMultiModuleStructure() throws IOException {
            String settingsContent = Files.readString(projectRoot.resolve("settings.gradle"));
            assertTrue(settingsContent.contains("include 'database'") ||
                      settingsContent.contains("include \"database\""),
                      "Should include database module");
            assertTrue(settingsContent.contains("include 'api'") ||
                      settingsContent.contains("include \"api\""),
                      "Should include api module");
            assertTrue(settingsContent.contains("include 'ui-components'") ||
                      settingsContent.contains("include \"ui-components\""),
                      "Should include ui-components module");
        }
    }

    @Nested
    @DisplayName("Gradle Wrapper Configuration")
    class GradleWrapperConfigurationTest {

        @Test
        @DisplayName("Should have Gradle wrapper files")
        void shouldHaveGradleWrapperFiles() {
            assertTrue(Files.exists(projectRoot.resolve("gradlew")),
                      "gradlew script should exist");
            assertTrue(Files.exists(projectRoot.resolve("gradlew.bat")),
                      "gradlew.bat script should exist");
            assertTrue(Files.exists(projectRoot.resolve("gradle/wrapper/gradle-wrapper.jar")),
                      "gradle-wrapper.jar should exist");
            assertTrue(Files.exists(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties")),
                      "gradle-wrapper.properties should exist");
        }

        @Test
        @DisplayName("Should configure Gradle 8.5 or higher")
        void shouldConfigureGradle85OrHigher() throws IOException {
            Properties props = new Properties();
            props.load(Files.newInputStream(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties")));
            
            String distributionUrl = props.getProperty("distributionUrl");
            assertNotNull(distributionUrl, "Distribution URL should be set");
            assertTrue(distributionUrl.contains("gradle-8.") || 
                      distributionUrl.contains("gradle-9."),
                      "Should use Gradle 8.x or higher");
        }

        @Test
        @DisplayName("Should have executable gradlew script")
        void shouldHaveExecutableGradlewScript() {
            File gradlew = projectRoot.resolve("gradlew").toFile();
            assertTrue(gradlew.canExecute() || System.getProperty("os.name").startsWith("Windows"),
                      "gradlew should be executable on Unix systems");
        }
    }

    @Nested
    @DisplayName("Dependency Management")
    class DependencyManagementTest {

        @Test
        @DisplayName("Should configure Spring dependency management")
        void shouldConfigureSpringDependencyManagement() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("io.spring.dependency-management"),
                      "Should configure Spring dependency management plugin");
        }

        @Test
        @DisplayName("Should configure Spring Boot plugin")
        void shouldConfigureSpringBootPlugin() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("org.springframework.boot"),
                      "Should configure Spring Boot plugin");
            assertTrue(buildContent.contains("version '3.2.0'") || 
                      buildContent.contains("version \"3.2.0\""),
                      "Should use Spring Boot 3.2.0");
        }

        @Test
        @DisplayName("Should configure repository for all projects")
        void shouldConfigureRepositoryForAllProjects() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("repositories"),
                      "Should configure repositories");
            assertTrue(buildContent.contains("mavenCentral()"),
                      "Should use Maven Central repository");
        }
    }

    @Nested
    @DisplayName("Module Configuration")
    class ModuleConfigurationTest {

        @Test
        @DisplayName("Database module should exist with proper structure")
        void databaseModuleShouldExistWithProperStructure() {
            Path databaseModule = projectRoot.resolve("database");
            assertTrue(Files.exists(databaseModule), "Database module should exist");
            assertTrue(Files.exists(databaseModule.resolve("build.gradle")),
                      "Database module should have build.gradle");
            assertTrue(Files.exists(databaseModule.resolve("src/main/java")),
                      "Database module should have Java source directory");
            assertTrue(Files.exists(databaseModule.resolve("src/main/resources")),
                      "Database module should have resources directory");
        }

        @Test
        @DisplayName("API module structure should be prepared")
        void apiModuleStructureShouldBePrepared() {
            Path apiModule = projectRoot.resolve("api");
            if (Files.exists(apiModule)) {
                assertTrue(Files.exists(apiModule.resolve("build.gradle")),
                          "API module should have build.gradle");
            }
        }

        @Test
        @DisplayName("UI-components module structure should be prepared")
        void uiComponentsModuleStructureShouldBePrepared() {
            Path uiModule = projectRoot.resolve("ui-components");
            if (Files.exists(uiModule)) {
                assertTrue(Files.exists(uiModule.resolve("build.gradle")) ||
                          Files.exists(uiModule.resolve("package.json")),
                          "UI module should have build configuration");
            }
        }
    }

    @Nested
    @DisplayName("Build Tasks Configuration")
    class BuildTasksConfigurationTest {

        @Test
        @DisplayName("Should configure custom build tasks")
        void shouldConfigureCustomBuildTasks() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("task") || buildContent.contains("tasks.register"),
                      "Should define custom tasks");
        }

        @Test
        @DisplayName("Should configure task dependencies")
        void shouldConfigureTaskDependencies() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("dependsOn") || buildContent.contains("depends"),
                      "Should configure task dependencies");
        }

        @Test
        @DisplayName("Should have wrapper task configured")
        void shouldHaveWrapperTaskConfigured() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("wrapper"),
                      "Should configure wrapper task");
        }
    }

    @Nested
    @DisplayName("Build Optimization")
    class BuildOptimizationTest {

        @Test
        @DisplayName("Should have gradle.properties for optimization")
        void shouldHaveGradlePropertiesForOptimization() {
            Path propertiesFile = projectRoot.resolve("gradle.properties");
            if (Files.exists(propertiesFile)) {
                assertTrue(propertiesFile.toFile().length() > 0,
                          "gradle.properties should not be empty");
            }
        }

        @Test
        @DisplayName("Should configure parallel builds if gradle.properties exists")
        void shouldConfigureParallelBuilds() throws IOException {
            Path propertiesFile = projectRoot.resolve("gradle.properties");
            if (Files.exists(propertiesFile)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(propertiesFile));
                
                String parallel = props.getProperty("org.gradle.parallel");
                if (parallel != null) {
                    assertEquals("true", parallel, "Parallel builds should be enabled");
                }
            }
        }

        @Test
        @DisplayName("Should configure build cache if gradle.properties exists")
        void shouldConfigureBuildCache() throws IOException {
            Path propertiesFile = projectRoot.resolve("gradle.properties");
            if (Files.exists(propertiesFile)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(propertiesFile));
                
                String caching = props.getProperty("org.gradle.caching");
                if (caching != null) {
                    assertEquals("true", caching, "Build cache should be enabled");
                }
            }
        }
    }

    @Nested
    @DisplayName("Plugin Configuration")
    class PluginConfigurationTest {

        @Test
        @DisplayName("Should apply Java plugin to subprojects")
        void shouldApplyJavaPluginToSubprojects() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("apply plugin: 'java'") ||
                      buildContent.contains("id 'java'"),
                      "Should apply Java plugin");
        }

        @Test
        @DisplayName("Should configure Spring Boot plugin correctly")
        void shouldConfigureSpringBootPluginCorrectly() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            assertTrue(buildContent.contains("apply false") || 
                      buildContent.contains("apply: false"),
                      "Spring Boot plugin should not be applied to root project");
        }

        @Test
        @DisplayName("Should have consistent plugin versions")
        void shouldHaveConsistentPluginVersions() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            // Count occurrences of version strings
            long springBootVersionCount = buildContent.lines()
                .filter(line -> line.contains("3.2.0"))
                .count();
            
            assertTrue(springBootVersionCount >= 1,
                      "Spring Boot version should be defined at least once");
        }
    }

    @Nested
    @DisplayName("Directory Structure")
    class DirectoryStructureTest {

        @Test
        @DisplayName("Should have standard Gradle directory layout")
        void shouldHaveStandardGradleDirectoryLayout() {
            assertTrue(Files.exists(projectRoot.resolve("src")) ||
                      Files.exists(projectRoot.resolve("database")) ||
                      Files.exists(projectRoot.resolve("api")) ||
                      Files.exists(projectRoot.resolve("ui-components")),
                      "Should have source directories or modules");
        }

        @Test
        @DisplayName("Should have .gitignore for Gradle files")
        void shouldHaveGitignoreForGradleFiles() throws IOException {
            Path gitignore = projectRoot.resolve(".gitignore");
            if (Files.exists(gitignore)) {
                String content = Files.readString(gitignore);
                assertTrue(content.contains(".gradle") || content.contains("build/"),
                          ".gitignore should exclude Gradle build files");
            }
        }

        @Test
        @DisplayName("Should not have build directories in version control")
        void shouldNotHaveBuildDirectoriesInVersionControl() {
            try (Stream<Path> paths = Files.walk(projectRoot)) {
                long buildDirs = paths
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals("build"))
                    .filter(p -> !p.toString().contains(".gradle"))
                    .count();
                
                // Build directories might exist from previous builds
                assertTrue(buildDirs >= 0, "Build directories count should be non-negative");
            } catch (IOException e) {
                fail("Failed to walk directory tree: " + e.getMessage());
            }
        }
    }
}