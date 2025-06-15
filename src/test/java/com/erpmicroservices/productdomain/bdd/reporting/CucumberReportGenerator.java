package com.erpmicroservices.productdomain.bdd.reporting;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.json.support.Status;
import net.masterthought.cucumber.presentation.PresentationMode;
import net.masterthought.cucumber.reducers.ReducingMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates comprehensive HTML reports from Cucumber JSON output.
 * Integrates with cucumber-reporting library for rich reporting features.
 */
public class CucumberReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CucumberReportGenerator.class);
    
    private final String outputDirectory;
    private final String projectName;
    private final String buildNumber;
    private final Map<String, String> classifications;

    public CucumberReportGenerator(String outputDirectory, String projectName, String buildNumber) {
        this.outputDirectory = outputDirectory;
        this.projectName = projectName;
        this.buildNumber = buildNumber != null ? buildNumber : generateBuildNumber();
        this.classifications = new LinkedHashMap<>();
        
        // Default classifications
        addClassification("Project", projectName);
        addClassification("Build", this.buildNumber);
        addClassification("Environment", System.getProperty("test.environment", "test"));
        addClassification("Platform", System.getProperty("os.name"));
        addClassification("Java Version", System.getProperty("java.version"));
        addClassification("Timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Generate HTML reports from Cucumber JSON files.
     */
    public void generateReports(List<String> jsonFiles) {
        try {
            File reportOutputDirectory = new File(outputDirectory);
            
            Configuration configuration = createConfiguration(reportOutputDirectory);
            
            ReportBuilder reportBuilder = new ReportBuilder(jsonFiles, configuration);
            reportBuilder.generateReports();
            
            logger.info("Cucumber reports generated at: {}", reportOutputDirectory.getAbsolutePath());
            
            // Generate additional custom reports
            generateCustomReports(reportOutputDirectory);
            
        } catch (Exception e) {
            logger.error("Failed to generate Cucumber reports", e);
            throw new ReportGenerationException("Report generation failed", e);
        }
    }

    /**
     * Generate reports from default location.
     */
    public void generateReports() {
        List<String> jsonFiles = findJsonReports("target/cucumber-reports");
        if (jsonFiles.isEmpty()) {
            logger.warn("No Cucumber JSON reports found to process");
            return;
        }
        generateReports(jsonFiles);
    }

    /**
     * Add classification information to the report.
     */
    public void addClassification(String name, String value) {
        classifications.put(name, value);
    }

    /**
     * Set custom report configuration.
     */
    public Configuration createConfiguration(File reportOutputDirectory) {
        Configuration configuration = new Configuration(reportOutputDirectory, projectName);
        
        // Add classifications
        classifications.forEach(configuration::addClassifications);
        
        // Configure report settings
        configuration.setBuildNumber(buildNumber);
        configuration.addPresentationModes(PresentationMode.RUN_WITH_JENKINS);
        configuration.addPresentationModes(PresentationMode.PARALLEL_TESTING);
        configuration.setNotFailingStatuses(Collections.singleton(Status.SKIPPED));
        
        // Configure trend reporting
        configuration.setTrendsStatsFile(new File(reportOutputDirectory, "trends.json"));
        
        // Configure reduction methods for cleaner reports
        configuration.addReducingMethod(ReducingMethod.HIDE_EMPTY_HOOKS);
        configuration.addReducingMethod(ReducingMethod.MERGE_FEATURES_WITH_RERUN);
        
        // Set custom properties
        configuration.addCustomJsFiles(createCustomJavaScript());
        configuration.addCustomCssFiles(createCustomCSS());
        
        return configuration;
    }

    /**
     * Generate additional custom reports.
     */
    private void generateCustomReports(File reportOutputDirectory) {
        try {
            // Generate test execution summary
            generateExecutionSummary(reportOutputDirectory);
            
            // Generate performance report
            generatePerformanceReport(reportOutputDirectory);
            
            // Generate failure analysis
            generateFailureAnalysis(reportOutputDirectory);
            
            // Generate tag statistics
            generateTagStatistics(reportOutputDirectory);
            
        } catch (Exception e) {
            logger.error("Failed to generate custom reports", e);
        }
    }

    /**
     * Generate execution summary report.
     */
    private void generateExecutionSummary(File outputDir) {
        // Implementation for execution summary
        logger.debug("Generating execution summary report");
    }

    /**
     * Generate performance report from timing data.
     */
    private void generatePerformanceReport(File outputDir) {
        // Implementation for performance report
        logger.debug("Generating performance report");
    }

    /**
     * Generate failure analysis report.
     */
    private void generateFailureAnalysis(File outputDir) {
        // Implementation for failure analysis
        logger.debug("Generating failure analysis report");
    }

    /**
     * Generate tag statistics report.
     */
    private void generateTagStatistics(File outputDir) {
        // Implementation for tag statistics
        logger.debug("Generating tag statistics report");
    }

    /**
     * Find all JSON report files in the given directory.
     */
    private List<String> findJsonReports(String directory) {
        List<String> jsonFiles = new ArrayList<>();
        File dir = new File(directory);
        
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    jsonFiles.add(file.getAbsolutePath());
                }
            }
        }
        
        return jsonFiles;
    }

    /**
     * Generate build number if not provided.
     */
    private String generateBuildNumber() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    /**
     * Create custom JavaScript for enhanced reporting.
     */
    private List<String> createCustomJavaScript() {
        // Could load from resources
        return Collections.emptyList();
    }

    /**
     * Create custom CSS for report styling.
     */
    private List<String> createCustomCSS() {
        // Could load from resources
        return Collections.emptyList();
    }

    /**
     * Main method for standalone report generation.
     */
    public static void main(String[] args) {
        String outputDir = args.length > 0 ? args[0] : "target/cucumber-html-reports";
        String projectName = args.length > 1 ? args[1] : "ProductDomain";
        String buildNumber = args.length > 2 ? args[2] : null;
        
        CucumberReportGenerator generator = new CucumberReportGenerator(
            outputDir, projectName, buildNumber
        );
        
        generator.generateReports();
    }
}

/**
 * Custom exception for report generation failures.
 */
class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message) {
        super(message);
    }
    
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}