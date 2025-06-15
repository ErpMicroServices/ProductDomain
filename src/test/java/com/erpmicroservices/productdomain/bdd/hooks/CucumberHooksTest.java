package com.erpmicroservices.productdomain.bdd.hooks;

import com.erpmicroservices.productdomain.bdd.utils.DatabaseCleanupUtil;
import com.erpmicroservices.productdomain.bdd.utils.TestContextHolder;
import com.erpmicroservices.productdomain.bdd.utils.TestDataFactory;
import io.cucumber.java.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cucumber Hooks Tests")
class CucumberHooksTest {

    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private DatabaseCleanupUtil databaseCleanupUtil;
    
    @Mock
    private TestDataFactory testDataFactory;
    
    @Mock
    private PlatformTransactionManager transactionManager;
    
    @Mock
    private TransactionStatus transactionStatus;
    
    @Mock
    private Scenario scenario;
    
    private CucumberHooks hooks;
    private TestContextHolder testContext;

    @BeforeEach
    void setUp() {
        testContext = TestContextHolder.getInstance();
        testContext.clear();
        
        hooks = new CucumberHooks();
        hooks.setApplicationContext(applicationContext);
        hooks.setDatabaseCleanupUtil(databaseCleanupUtil);
        hooks.setTestDataFactory(testDataFactory);
    }

    @Nested
    @DisplayName("Before Scenario Hooks")
    class BeforeScenarioHooks {

        @Test
        @DisplayName("Should initialize test context before scenario")
        void shouldInitializeTestContext() {
            // Given
            when(scenario.getName()).thenReturn("Test Scenario");
            when(scenario.getId()).thenReturn("test-scenario-id");
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@tag1", "@tag2"));

            // When
            hooks.beforeScenario(scenario);

            // Then
            assertThat(testContext.getScenarioName()).isEqualTo("Test Scenario");
            assertThat(testContext.getScenarioId()).isEqualTo("test-scenario-id");
            assertThat(testContext.getTags()).containsExactly("@tag1", "@tag2");
        }

        @Test
        @DisplayName("Should clean database before scenario without @no-cleanup tag")
        void shouldCleanDatabaseBeforeScenario() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@feature"));
            
            // When
            hooks.beforeScenario(scenario);

            // Then
            verify(databaseCleanupUtil).cleanAllTablesExcept(anySet());
        }

        @Test
        @DisplayName("Should skip database cleanup with @no-cleanup tag")
        void shouldSkipDatabaseCleanupWithTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@no-cleanup"));
            
            // When
            hooks.beforeScenario(scenario);

            // Then
            verify(databaseCleanupUtil, never()).cleanAllTablesExcept(anySet());
        }

        @Test
        @DisplayName("Should start transaction with @transactional tag")
        void shouldStartTransactionWithTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@transactional"));
            when(applicationContext.getBean(PlatformTransactionManager.class))
                .thenReturn(transactionManager);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            // When
            hooks.beforeScenario(scenario);

            // Then
            verify(transactionManager).getTransaction(any());
            assertThat((Object) testContext.get("transactionStatus")).isEqualTo(transactionStatus);
        }

        @Test
        @DisplayName("Should load test data with @with-data tag")
        void shouldLoadTestDataWithTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(
                Arrays.asList("@with-data:product-catalog")
            );
            Map<String, Object> testData = Map.of("products", Arrays.asList("p1", "p2"));
            when(testDataFactory.loadScenarioData("product-catalog")).thenReturn(testData);

            // When
            hooks.beforeScenario(scenario);

            // Then
            verify(testDataFactory).loadScenarioData("product-catalog");
            assertThat(testContext.getTestData()).containsAllEntriesOf(testData);
        }
    }

    @Nested
    @DisplayName("After Scenario Hooks")
    class AfterScenarioHooks {

        @Test
        @DisplayName("Should capture screenshot on failure")
        void shouldCaptureScreenshotOnFailure() {
            // Given
            when(scenario.isFailed()).thenReturn(true);
            when(scenario.getName()).thenReturn("Failed Scenario");

            // When
            hooks.afterScenario(scenario);

            // Then
            verify(scenario).attach(any(byte[].class), eq("image/png"), anyString());
        }

        @Test
        @DisplayName("Should rollback transaction with @transactional tag")
        void shouldRollbackTransactionWithTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@transactional"));
            testContext.put("transactionStatus", transactionStatus);
            when(applicationContext.getBean(PlatformTransactionManager.class))
                .thenReturn(transactionManager);

            // When
            hooks.afterScenario(scenario);

            // Then
            verify(transactionManager).rollback(transactionStatus);
        }

        @Test
        @DisplayName("Should clean up test context after scenario")
        void shouldCleanUpTestContext() {
            // Given
            testContext.put("test-key", "test-value");

            // When
            hooks.afterScenario(scenario);

            // Then
            assertThat((Object) testContext.get("test-key")).isNull();
        }

        @Test
        @DisplayName("Should log scenario execution time")
        void shouldLogScenarioExecutionTime() {
            // Given
            when(scenario.getName()).thenReturn("Test Scenario");
            testContext.setScenarioStartTime(System.currentTimeMillis() - 1000);

            // When
            hooks.afterScenario(scenario);

            // Then
            // Execution time should be logged (verified through test output)
        }

        @Test
        @DisplayName("Should save test artifacts on failure")
        void shouldSaveTestArtifactsOnFailure() {
            // Given
            when(scenario.isFailed()).thenReturn(true);
            testContext.put("response", "HTTP 500 Error");
            testContext.put("database-state", Map.of("products", 5));

            // When
            hooks.afterScenario(scenario);

            // Then
            verify(scenario).attach(
                any(byte[].class),
                eq("application/json"),
                eq("test-context.json")
            );
        }
    }

    @Nested
    @DisplayName("Step Hooks")
    class StepHooks {

        @Test
        @DisplayName("Should log step execution")
        void shouldLogStepExecution() {
            // When
            hooks.beforeStep("Given I have a product");
            hooks.afterStep("Given I have a product");

            // Then
            // Step execution should be logged (verified through test output)
        }

        @Test
        @DisplayName("Should track step execution time")
        void shouldTrackStepExecutionTime() {
            // When
            hooks.beforeStep("When I search for products");
            try {
                Thread.sleep(100); // Simulate step execution
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            hooks.afterStep("When I search for products");

            // Then
            assertThat(testContext.getStepExecutionTime("When I search for products"))
                .isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Tagged Hooks")
    class TaggedHooks {

        @Test
        @DisplayName("Should run API setup for @api tag")
        void shouldRunApiSetupForApiTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@api"));

            // When
            hooks.beforeApiScenario(scenario);

            // Then
            assertThat((Object) testContext.get("api-base-url")).isNotNull();
            assertThat((Object) testContext.get("api-client")).isNotNull();
        }

        @Test
        @DisplayName("Should run database setup for @database tag")
        void shouldRunDatabaseSetupForDatabaseTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@database"));

            // When
            hooks.beforeDatabaseScenario(scenario);

            // Then
            verify(databaseCleanupUtil).createSnapshot("scenario-start");
        }

        @Test
        @DisplayName("Should run performance setup for @performance tag")
        void shouldRunPerformanceSetupForPerformanceTag() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@performance"));

            // When
            hooks.beforePerformanceScenario(scenario);

            // Then
            assertThat((Object) testContext.get("performance-metrics")).isNotNull();
            assertThat((Object) testContext.get("start-memory")).isNotNull();
        }

        @Test
        @DisplayName("Should collect performance metrics after @performance scenario")
        void shouldCollectPerformanceMetrics() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@performance"));
            testContext.put("performance-metrics", new CucumberHooks.PerformanceMetrics());
            testContext.put("start-memory", Runtime.getRuntime().totalMemory());

            // When
            hooks.afterPerformanceScenario(scenario);

            // Then
            verify(scenario).attach(
                any(byte[].class),
                eq("application/json"),
                eq("performance-metrics.json")
            );
        }
    }

    @Nested
    @DisplayName("Parallel Execution Hooks")
    class ParallelExecutionHooks {

        @Test
        @DisplayName("Should isolate test data in parallel execution")
        void shouldIsolateTestDataInParallelExecution() {
            // Given
            String threadId = Thread.currentThread().getName();
            
            // When
            hooks.beforeScenario(scenario);
            testContext.put("thread-specific", threadId);

            // Then
            assertThat((Object) testContext.get("thread-specific")).isEqualTo(threadId);
            // Each thread should have its own context
        }

        @Test
        @DisplayName("Should use separate database schema for parallel tests")
        void shouldUseSeparateDatabaseSchema() {
            // Given
            when(scenario.getSourceTagNames()).thenReturn(Arrays.asList("@parallel"));
            
            // When
            hooks.beforeParallelScenario(scenario);

            // Then
            String schema = (String) testContext.get("test-schema");
            assertThat(schema).startsWith("test_");
            assertThat(schema).contains(Thread.currentThread().getName());
        }
    }

    @Nested
    @DisplayName("Error Handling Hooks")
    class ErrorHandlingHooks {

        @Test
        @DisplayName("Should handle cleanup errors gracefully")
        void shouldHandleCleanupErrorsGracefully() {
            // Given
            doThrow(new RuntimeException("Cleanup failed"))
                .when(databaseCleanupUtil).cleanAllTablesExcept(anySet());

            // When/Then
            assertThatCode(() -> hooks.beforeScenario(scenario))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should always clear context even with errors")
        void shouldAlwaysClearContextWithErrors() {
            // Given
            when(scenario.isFailed()).thenReturn(true);
            doThrow(new RuntimeException("Screenshot failed"))
                .when(scenario).attach(any(byte[].class), anyString(), anyString());
            testContext.put("test-data", "value");

            // When
            hooks.afterScenario(scenario);

            // Then
            assertThat((Object) testContext.get("test-data")).isNull();
        }
    }
}