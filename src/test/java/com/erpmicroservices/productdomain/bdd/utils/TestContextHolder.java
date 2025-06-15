package com.erpmicroservices.productdomain.bdd.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe context holder for sharing data between Cucumber steps.
 * Uses ThreadLocal to ensure test isolation in parallel execution.
 */
public class TestContextHolder {

    private static final ThreadLocal<TestContextHolder> INSTANCE = 
        ThreadLocal.withInitial(TestContextHolder::new);
    
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final Map<String, Long> stepTimings = new ConcurrentHashMap<>();
    
    // Scenario metadata
    private String scenarioName;
    private String scenarioId;
    private Collection<String> tags;
    private long scenarioStartTime;
    private long scenarioEndTime;

    private TestContextHolder() {
        // Private constructor for singleton per thread
    }

    /**
     * Get the thread-local instance of TestContextHolder.
     */
    public static TestContextHolder getInstance() {
        return INSTANCE.get();
    }

    /**
     * Store a value in the context.
     */
    public void put(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Retrieve a value from the context.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) context.get(key);
    }

    /**
     * Retrieve a value with a default if not present.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    /**
     * Check if a key exists in the context.
     */
    public boolean containsKey(String key) {
        return context.containsKey(key);
    }

    /**
     * Remove a value from the context.
     */
    public void remove(String key) {
        context.remove(key);
    }

    /**
     * Clear all context data.
     */
    public void clear() {
        context.clear();
        stepTimings.clear();
        scenarioName = null;
        scenarioId = null;
        tags = null;
        scenarioStartTime = 0;
        scenarioEndTime = 0;
    }

    /**
     * Get all test data stored in context.
     */
    public Map<String, Object> getTestData() {
        return new HashMap<>(context);
    }

    /**
     * Add multiple test data entries.
     */
    public void putAll(Map<String, Object> data) {
        context.putAll(data);
    }

    // Scenario metadata methods

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public Collection<String> getTags() {
        return tags != null ? new ArrayList<>(tags) : Collections.emptyList();
    }

    public void setTags(Collection<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public long getScenarioStartTime() {
        return scenarioStartTime;
    }

    public void setScenarioStartTime(long scenarioStartTime) {
        this.scenarioStartTime = scenarioStartTime;
    }

    public long getScenarioEndTime() {
        return scenarioEndTime;
    }

    public void setScenarioEndTime(long scenarioEndTime) {
        this.scenarioEndTime = scenarioEndTime;
    }

    public long getScenarioExecutionTime() {
        return scenarioEndTime - scenarioStartTime;
    }

    // Step timing methods

    public void recordStepStart(String stepName) {
        stepTimings.put(stepName + "_start", System.currentTimeMillis());
    }

    public void recordStepEnd(String stepName) {
        stepTimings.put(stepName + "_end", System.currentTimeMillis());
    }

    public long getStepExecutionTime(String stepName) {
        Long start = stepTimings.get(stepName + "_start");
        Long end = stepTimings.get(stepName + "_end");
        
        if (start != null && end != null) {
            return end - start;
        }
        return 0;
    }

    public Map<String, Long> getAllStepTimings() {
        Map<String, Long> timings = new HashMap<>();
        
        stepTimings.forEach((key, value) -> {
            if (key.endsWith("_start")) {
                String stepName = key.substring(0, key.length() - 6);
                timings.put(stepName, getStepExecutionTime(stepName));
            }
        });
        
        return timings;
    }

    // Utility methods for common test data

    /**
     * Store a response object (e.g., from API calls).
     */
    public void setResponse(Object response) {
        put("response", response);
    }

    /**
     * Get the stored response.
     */
    public <T> T getResponse() {
        return get("response");
    }

    /**
     * Store an error/exception.
     */
    public void setError(Throwable error) {
        put("error", error);
    }

    /**
     * Get the stored error.
     */
    public Throwable getError() {
        return get("error");
    }

    /**
     * Store a list of created entities for cleanup.
     */
    @SuppressWarnings("unchecked")
    public void addCreatedEntity(String type, Object id) {
        String key = "created_" + type;
        List<Object> entities = (List<Object>) context.computeIfAbsent(key, k -> new ArrayList<>());
        entities.add(id);
    }

    /**
     * Get all created entities of a specific type.
     */
    @SuppressWarnings("unchecked")
    public List<Object> getCreatedEntities(String type) {
        String key = "created_" + type;
        return (List<Object>) context.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Store test user information.
     */
    public void setTestUser(String username, String password, String token) {
        Map<String, String> user = new HashMap<>();
        user.put("username", username);
        user.put("password", password);
        user.put("token", token);
        put("test_user", user);
    }

    /**
     * Get test user information.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getTestUser() {
        return (Map<String, String>) get("test_user");
    }

    /**
     * Check if a specific tag is present.
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    /**
     * Get a tag value (for tags like @with-data:dataset-name).
     */
    public String getTagValue(String tagPrefix) {
        if (tags == null) {
            return null;
        }
        
        return tags.stream()
            .filter(tag -> tag.startsWith(tagPrefix + ":"))
            .map(tag -> tag.substring(tagPrefix.length() + 1))
            .findFirst()
            .orElse(null);
    }

    /**
     * Store performance metrics.
     */
    public void recordPerformanceMetric(String metric, double value) {
        @SuppressWarnings("unchecked")
        Map<String, Double> metrics = (Map<String, Double>) 
            context.computeIfAbsent("performance_metrics", k -> new HashMap<>());
        metrics.put(metric, value);
    }

    /**
     * Get all performance metrics.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> getPerformanceMetrics() {
        return (Map<String, Double>) context.getOrDefault("performance_metrics", Collections.emptyMap());
    }

    /**
     * Create a snapshot of current context state.
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("context", new HashMap<>(context));
        snapshot.put("scenarioName", scenarioName);
        snapshot.put("scenarioId", scenarioId);
        snapshot.put("tags", tags != null ? new ArrayList<>(tags) : null);
        snapshot.put("stepTimings", new HashMap<>(stepTimings));
        return snapshot;
    }

    /**
     * Restore context from a snapshot.
     */
    @SuppressWarnings("unchecked")
    public void restore(Map<String, Object> snapshot) {
        clear();
        
        if (snapshot.containsKey("context")) {
            context.putAll((Map<String, Object>) snapshot.get("context"));
        }
        
        scenarioName = (String) snapshot.get("scenarioName");
        scenarioId = (String) snapshot.get("scenarioId");
        tags = (Collection<String>) snapshot.get("tags");
        
        if (snapshot.containsKey("stepTimings")) {
            stepTimings.putAll((Map<String, Long>) snapshot.get("stepTimings"));
        }
    }

    @Override
    public String toString() {
        return String.format(
            "TestContext[scenario=%s, tags=%s, data=%d items]",
            scenarioName,
            tags,
            context.size()
        );
    }
}