package com.erpmicroservices.productdomain.bdd.hooks;

import com.erpmicroservices.productdomain.bdd.utils.DatabaseCleanupUtil;
import com.erpmicroservices.productdomain.bdd.utils.TestContextHolder;
import com.erpmicroservices.productdomain.bdd.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.*;
import io.cucumber.spring.CucumberContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Global Cucumber hooks for test lifecycle management.
 * Handles database cleanup, test context, and reporting.
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberHooks {

    private static final Logger logger = LoggerFactory.getLogger(CucumberHooks.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private DatabaseCleanupUtil databaseCleanupUtil;
    
    @Autowired
    private TestDataFactory testDataFactory;
    
    private final TestContextHolder testContext = TestContextHolder.getInstance();
    
    // Package-private setters for testing
    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    void setDatabaseCleanupUtil(DatabaseCleanupUtil databaseCleanupUtil) {
        this.databaseCleanupUtil = databaseCleanupUtil;
    }
    
    void setTestDataFactory(TestDataFactory testDataFactory) {
        this.testDataFactory = testDataFactory;
    }

    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {
        logger.info("Starting scenario: {}", scenario.getName());
        
        // Initialize test context
        testContext.clear();
        testContext.setScenarioName(scenario.getName());
        testContext.setScenarioId(scenario.getId());
        testContext.setTags(scenario.getSourceTagNames());
        testContext.setScenarioStartTime(System.currentTimeMillis());
        
        // Database cleanup unless tagged with @no-cleanup
        if (!testContext.hasTag("@no-cleanup")) {
            try {
                databaseCleanupUtil.cleanAllTablesExcept(Set.of("users", "roles", "permissions"));
                logger.debug("Database cleaned for scenario");
            } catch (Exception e) {
                logger.error("Failed to clean database", e);
                // Continue with test execution
            }
        }
        
        // Start transaction if tagged with @transactional
        if (testContext.hasTag("@transactional")) {
            startTransaction();
        }
        
        // Load test data if tagged with @with-data
        String dataSet = testContext.getTagValue("@with-data");
        if (dataSet != null) {
            Map<String, Object> testData = testDataFactory.loadScenarioData(dataSet);
            testContext.putAll(testData);
            logger.debug("Loaded test data set: {}", dataSet);
        }
    }

    @After(order = 0)
    public void afterScenario(Scenario scenario) {
        testContext.setScenarioEndTime(System.currentTimeMillis());
        
        try {
            // Capture screenshot/data on failure
            if (scenario.isFailed()) {
                captureFailureArtifacts(scenario);
            }
            
            // Rollback transaction if active
            if (testContext.hasTag("@transactional")) {
                rollbackTransaction();
            }
            
            // Log execution time
            long executionTime = testContext.getScenarioExecutionTime();
            logger.info("Scenario '{}' completed in {}ms", scenario.getName(), executionTime);
            
            // Attach execution report
            attachExecutionReport(scenario);
            
        } finally {
            // Always clear context
            testContext.clear();
        }
    }

    @BeforeStep
    public void beforeStep(String stepName) {
        testContext.recordStepStart(stepName);
        logger.trace("Starting step: {}", stepName);
    }

    @AfterStep
    public void afterStep(String stepName) {
        testContext.recordStepEnd(stepName);
        long stepTime = testContext.getStepExecutionTime(stepName);
        
        if (stepTime > 1000) {
            logger.warn("Step '{}' took {}ms", stepName, stepTime);
        } else {
            logger.trace("Step '{}' completed in {}ms", stepName, stepTime);
        }
    }

    // Tagged hooks for specific scenarios

    @Before("@api")
    public void beforeApiScenario(Scenario scenario) {
        logger.debug("Setting up API test context");
        
        // Initialize API client
        testContext.put("api-base-url", getApiBaseUrl());
        testContext.put("api-client", createApiClient());
        
        // Set default headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        testContext.put("api-headers", headers);
    }

    @Before("@database")
    public void beforeDatabaseScenario(Scenario scenario) {
        logger.debug("Setting up database test context");
        
        // Create database snapshot
        databaseCleanupUtil.createSnapshot("scenario-start");
        testContext.put("database-snapshot", "scenario-start");
    }

    @After("@database")
    public void afterDatabaseScenario(Scenario scenario) {
        // Optionally restore database state
        if (scenario.isFailed() && testContext.hasTag("@restore-on-failure")) {
            String snapshot = testContext.get("database-snapshot");
            if (snapshot != null) {
                databaseCleanupUtil.restoreSnapshot(snapshot);
                databaseCleanupUtil.deleteSnapshot(snapshot);
            }
        }
    }

    @Before("@performance")
    public void beforePerformanceScenario(Scenario scenario) {
        logger.debug("Setting up performance monitoring");
        
        // Initialize performance metrics
        testContext.put("performance-metrics", new PerformanceMetrics());
        testContext.put("start-memory", Runtime.getRuntime().totalMemory());
        testContext.put("start-threads", Thread.activeCount());
    }

    @After("@performance")
    public void afterPerformanceScenario(Scenario scenario) {
        PerformanceMetrics metrics = testContext.get("performance-metrics");
        if (metrics != null) {
            // Calculate final metrics
            long endMemory = Runtime.getRuntime().totalMemory();
            long startMemory = testContext.get("start-memory", 0L);
            metrics.setMemoryUsed(endMemory - startMemory);
            metrics.setExecutionTime(testContext.getScenarioExecutionTime());
            
            // Attach performance report
            try {
                String metricsJson = objectMapper.writeValueAsString(metrics);
                scenario.attach(metricsJson.getBytes(StandardCharsets.UTF_8), 
                    "application/json", "performance-metrics.json");
            } catch (Exception e) {
                logger.error("Failed to attach performance metrics", e);
            }
        }
    }

    @Before("@parallel")
    public void beforeParallelScenario(Scenario scenario) {
        // Setup for parallel execution
        String threadName = Thread.currentThread().getName();
        String testSchema = "test_" + threadName.replaceAll("[^a-zA-Z0-9]", "_");
        testContext.put("test-schema", testSchema);
        
        logger.debug("Running scenario in parallel with schema: {}", testSchema);
    }

    // Private helper methods

    private void startTransaction() {
        PlatformTransactionManager txManager = applicationContext.getBean(PlatformTransactionManager.class);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = txManager.getTransaction(def);
        testContext.put("transactionStatus", status);
        testContext.put("transactionManager", txManager);
        logger.debug("Started transaction for scenario");
    }

    private void rollbackTransaction() {
        TransactionStatus status = testContext.get("transactionStatus");
        PlatformTransactionManager txManager = testContext.get("transactionManager");
        
        if (status != null && txManager != null) {
            txManager.rollback(status);
            logger.debug("Rolled back transaction for scenario");
        }
    }

    private void captureFailureArtifacts(Scenario scenario) {
        try {
            // Capture screenshot (mock for non-UI tests)
            byte[] screenshot = captureScreenshot();
            scenario.attach(screenshot, "image/png", "failure-screenshot.png");
            
            // Capture test context
            Map<String, Object> contextData = testContext.snapshot();
            String contextJson = objectMapper.writeValueAsString(contextData);
            scenario.attach(contextJson.getBytes(StandardCharsets.UTF_8), 
                "application/json", "test-context.json");
            
            // Capture any error details
            Throwable error = testContext.getError();
            if (error != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                ps.println(error.getMessage());
                ps.println("Stack trace:");
                for (StackTraceElement element : error.getStackTrace()) {
                    ps.println("  at " + element);
                }
                scenario.attach(baos.toByteArray(), "text/plain", "error-stacktrace.txt");
            }
        } catch (Exception e) {
            logger.error("Failed to capture failure artifacts", e);
        }
    }

    private byte[] captureScreenshot() {
        // Mock screenshot for non-UI tests
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String mockScreenshot = String.format(
            "Mock screenshot captured at %s\nScenario: %s\nFailed at step: N/A",
            timestamp,
            testContext.getScenarioName()
        );
        return mockScreenshot.getBytes(StandardCharsets.UTF_8);
    }

    private void attachExecutionReport(Scenario scenario) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("scenario", scenario.getName());
            report.put("status", scenario.getStatus());
            report.put("tags", testContext.getTags());
            report.put("executionTime", testContext.getScenarioExecutionTime());
            report.put("stepTimings", testContext.getAllStepTimings());
            
            String reportJson = objectMapper.writeValueAsString(report);
            scenario.attach(reportJson.getBytes(StandardCharsets.UTF_8), 
                "application/json", "execution-report.json");
        } catch (Exception e) {
            logger.error("Failed to attach execution report", e);
        }
    }

    private String getApiBaseUrl() {
        // Get from configuration or environment
        return System.getProperty("api.base.url", "http://localhost:8080");
    }

    private Object createApiClient() {
        // Create API client (placeholder)
        return new Object(); // Replace with actual API client
    }

    /**
     * Performance metrics container.
     */
    static class PerformanceMetrics {
        private long executionTime;
        private long memoryUsed;
        private int threadCount;
        private Map<String, Double> customMetrics = new HashMap<>();

        // Getters and setters
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        
        public long getMemoryUsed() { return memoryUsed; }
        public void setMemoryUsed(long memoryUsed) { this.memoryUsed = memoryUsed; }
        
        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
        
        public Map<String, Double> getCustomMetrics() { return customMetrics; }
        public void setCustomMetrics(Map<String, Double> customMetrics) { this.customMetrics = customMetrics; }
        
        public void addMetric(String name, double value) {
            customMetrics.put(name, value);
        }
    }
}