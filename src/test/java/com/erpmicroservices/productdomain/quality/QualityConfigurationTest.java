package com.erpmicroservices.productdomain.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Code Quality Configuration Tests")
class QualityConfigurationTest {

    private Path projectRoot;
    
    @BeforeEach
    void setUp() {
        projectRoot = Path.of(System.getProperty("user.dir"));
    }

    @Nested
    @DisplayName("Checkstyle Configuration")
    class CheckstyleConfigurationTest {
        
        @Test
        @DisplayName("Should have valid Checkstyle XML configuration")
        void shouldHaveValidCheckstyleXmlConfiguration() throws ParserConfigurationException, IOException, SAXException {
            File checkstyleConfig = projectRoot.resolve("config/checkstyle/checkstyle.xml").toFile();
            assertTrue(checkstyleConfig.exists(), "Checkstyle config should exist");
            
            // Validate XML structure
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(checkstyleConfig);
            doc.getDocumentElement().normalize();
            
            assertEquals("module", doc.getDocumentElement().getNodeName(), 
                        "Root element should be 'module'");
            assertEquals("Checker", doc.getDocumentElement().getAttribute("name"), 
                        "Root module should be 'Checker'");
        }

        @Test
        @DisplayName("Should configure essential Checkstyle rules")
        void shouldConfigureEssentialCheckstyleRules() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/checkstyle/checkstyle.xml"));
            
            // Essential rules that should be present
            assertTrue(content.contains("FileTabCharacter"), "Should check for tab characters");
            assertTrue(content.contains("LineLength"), "Should check line length");
            assertTrue(content.contains("MethodLength"), "Should check method length");
            assertTrue(content.contains("ParameterNumber"), "Should check parameter count");
            assertTrue(content.contains("UnusedImports"), "Should check unused imports");
            assertTrue(content.contains("NeedBraces"), "Should enforce braces");
            assertTrue(content.contains("EqualsHashCode"), "Should check equals/hashCode");
        }

        @Test
        @DisplayName("Should have reasonable limits configured")
        void shouldHaveReasonableLimitsConfigured() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/checkstyle/checkstyle.xml"));
            
            // Check line length limit
            assertTrue(content.contains("LineLength") && content.contains("max"), 
                      "Should configure max line length");
            
            // Check method length limit
            assertTrue(content.contains("MethodLength") && content.contains("max"), 
                      "Should configure max method length");
        }
    }

    @Nested
    @DisplayName("PMD Configuration")
    class PMDConfigurationTest {
        
        @Test
        @DisplayName("Should have valid PMD ruleset XML")
        void shouldHaveValidPmdRulesetXml() throws ParserConfigurationException, IOException, SAXException {
            File pmdRuleset = projectRoot.resolve("config/pmd/ruleset.xml").toFile();
            assertTrue(pmdRuleset.exists(), "PMD ruleset should exist");
            
            // Validate XML structure
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pmdRuleset);
            doc.getDocumentElement().normalize();
            
            assertEquals("ruleset", doc.getDocumentElement().getNodeName(), 
                        "Root element should be 'ruleset'");
        }

        @Test
        @DisplayName("Should include essential PMD rule categories")
        void shouldIncludeEssentialPmdRuleCategories() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/pmd/ruleset.xml"));
            
            // Essential rule categories
            assertTrue(content.contains("bestpractices"), "Should include best practices rules");
            assertTrue(content.contains("codestyle"), "Should include code style rules");
            assertTrue(content.contains("design"), "Should include design rules");
            assertTrue(content.contains("errorprone"), "Should include error prone rules");
            assertTrue(content.contains("performance"), "Should include performance rules");
            assertTrue(content.contains("security"), "Should include security rules");
        }

        @Test
        @DisplayName("Should exclude appropriate PMD rules")
        void shouldExcludeAppropriatePmdRules() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/pmd/ruleset.xml"));
            
            // Common rules to exclude for practical reasons
            assertTrue(content.contains("exclude"), "Should exclude some rules");
            assertTrue(content.contains("OnlyOneReturn") || 
                      content.contains("LawOfDemeter") ||
                      content.contains("TooManyStaticImports"),
                      "Should exclude overly strict rules");
        }
    }

    @Nested
    @DisplayName("SpotBugs Configuration")
    class SpotBugsConfigurationTest {
        
        @Test
        @DisplayName("Should have valid SpotBugs exclude filter")
        void shouldHaveValidSpotBugsExcludeFilter() throws ParserConfigurationException, IOException, SAXException {
            File spotbugsExclude = projectRoot.resolve("config/spotbugs/exclude.xml").toFile();
            assertTrue(spotbugsExclude.exists(), "SpotBugs exclude filter should exist");
            
            // Validate XML structure
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(spotbugsExclude);
            doc.getDocumentElement().normalize();
            
            assertEquals("FindBugsFilter", doc.getDocumentElement().getNodeName(), 
                        "Root element should be 'FindBugsFilter'");
        }

        @Test
        @DisplayName("Should exclude generated code from SpotBugs")
        void shouldExcludeGeneratedCodeFromSpotBugs() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/spotbugs/exclude.xml"));
            
            assertTrue(content.contains("generated") || content.contains("Generated"),
                      "Should exclude generated code");
            assertTrue(content.contains("Lombok") || content.contains("_"),
                      "Should exclude Lombok generated code");
        }

        @Test
        @DisplayName("Should exclude test classes from certain SpotBugs checks")
        void shouldExcludeTestClassesFromCertainSpotBugsChecks() throws IOException {
            String content = Files.readString(projectRoot.resolve("config/spotbugs/exclude.xml"));
            
            assertTrue(content.contains("Test") && content.contains("Match"),
                      "Should have test class exclusions");
        }
    }

    @Nested
    @DisplayName("Build Integration")
    class BuildIntegrationTest {
        
        @Test
        @DisplayName("Should have quality plugins configured in build.gradle")
        void shouldHaveQualityPluginsConfiguredInBuildGradle() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("checkstyle"), "Should have Checkstyle plugin");
            assertTrue(buildContent.contains("pmd"), "Should have PMD plugin");
            assertTrue(buildContent.contains("spotbugs"), "Should have SpotBugs plugin");
            assertTrue(buildContent.contains("jacoco"), "Should have JaCoCo plugin");
        }

        @Test
        @DisplayName("Should configure quality tools to fail on violations")
        void shouldConfigureQualityToolsToFailOnViolations() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("ignoreFailures = false"),
                      "Quality tools should not ignore failures");
            assertTrue(buildContent.contains("maxWarnings = 0") || 
                      buildContent.contains("maxWarnings: 0"),
                      "Should fail on warnings");
        }

        @Test
        @DisplayName("Should generate HTML reports for all quality tools")
        void shouldGenerateHtmlReportsForAllQualityTools() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("html") && buildContent.contains("required = true"),
                      "Should generate HTML reports");
        }
    }

    @Nested
    @DisplayName("ESLint Configuration")
    class ESLintConfigurationTest {
        
        @Test
        @DisplayName("Should have ESLint configuration for UI components")
        void shouldHaveEslintConfigurationForUiComponents() {
            File eslintConfig = projectRoot.resolve("ui-components/.eslintrc.cjs").toFile();
            assertTrue(eslintConfig.exists(), "ESLint config should exist");
            assertTrue(eslintConfig.length() > 0, "ESLint config should not be empty");
        }

        @Test
        @DisplayName("Should configure React and TypeScript ESLint rules")
        void shouldConfigureReactAndTypeScriptEslintRules() throws IOException {
            Path eslintPath = projectRoot.resolve("ui-components/.eslintrc.cjs");
            if (Files.exists(eslintPath)) {
                String content = Files.readString(eslintPath);
                
                assertTrue(content.contains("typescript"), "Should have TypeScript ESLint plugin");
                assertTrue(content.contains("react"), "Should have React ESLint plugin");
                assertTrue(content.contains("react-hooks"), "Should have React Hooks ESLint plugin");
            }
        }

        @Test
        @DisplayName("Should have Prettier configuration")
        void shouldHavePrettierConfiguration() {
            File prettierConfig = projectRoot.resolve("ui-components/.prettierrc").toFile();
            assertTrue(prettierConfig.exists(), "Prettier config should exist");
        }
    }

    @Nested
    @DisplayName("JaCoCo Coverage Configuration")
    class JaCoCoCoverageConfigurationTest {
        
        @Test
        @DisplayName("Should configure JaCoCo for test coverage")
        void shouldConfigureJacocoForTestCoverage() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("jacoco"), "Should have JaCoCo plugin");
            assertTrue(buildContent.contains("jacocoTestReport"), "Should configure test report task");
        }

        @Test
        @DisplayName("Should generate both XML and HTML coverage reports")
        void shouldGenerateBothXmlAndHtmlCoverageReports() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("xml.required = true"), 
                      "Should generate XML coverage report");
            assertTrue(buildContent.contains("html.required = true"), 
                      "Should generate HTML coverage report");
        }

        @Test
        @DisplayName("Should configure coverage report aggregation")
        void shouldConfigureCoverageReportAggregation() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("jacocoRootReport"), 
                      "Should have root coverage report task");
        }
    }

    @Nested
    @DisplayName("Quality Gate Tasks")
    class QualityGateTasksTest {
        
        @Test
        @DisplayName("Should have checkAll task for running all quality checks")
        void shouldHaveCheckAllTaskForRunningAllQualityChecks() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("checkAll"), "Should have checkAll task");
            assertTrue(buildContent.contains("Run all quality checks"), 
                      "checkAll task should have proper description");
        }

        @Test
        @DisplayName("Should link quality checks to test task")
        void shouldLinkQualityChecksToTestTask() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("finalizedBy jacocoTestReport"), 
                      "Test task should trigger coverage report");
        }

        @Test
        @DisplayName("Should configure proper task dependencies")
        void shouldConfigureProperTaskDependencies() throws IOException {
            String buildContent = Files.readString(projectRoot.resolve("build.gradle"));
            
            assertTrue(buildContent.contains("dependsOn"), 
                      "Tasks should have proper dependencies");
        }
    }

    @Nested
    @DisplayName("Pre-commit Hooks")
    class PreCommitHooksTest {
        
        @Test
        @DisplayName("Should have hook configuration files")
        void shouldHaveHookConfigurationFiles() {
            // Check for various pre-commit hook systems
            assertTrue(
                Files.exists(projectRoot.resolve(".husky")) ||
                Files.exists(projectRoot.resolve(".pre-commit-config.yaml")) ||
                Files.exists(projectRoot.resolve("gradle/git-hooks")) ||
                Files.exists(projectRoot.resolve("scripts/install-hooks.sh")),
                "Should have pre-commit hook configuration"
            );
        }

        @Test
        @DisplayName("Should have Git hooks directory")
        void shouldHaveGitHooksDirectory() {
            Path gitHooks = projectRoot.resolve(".git/hooks");
            if (Files.exists(projectRoot.resolve(".git"))) {
                assertTrue(Files.exists(gitHooks), "Git hooks directory should exist");
            }
        }
    }
}