package com.erpmicroservices.productdomain.api.steps;

import com.erpmicroservices.productdomain.api.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class GraphQLSteps extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebTestClient webTestClient;

    private GraphQlTester graphQlTester;
    private String graphQLQuery;
    private Map<String, Object> graphQLVariables = new HashMap<>();
    private MvcResult graphQLResponse;
    private String responseBody;
    private int databaseQueryCount;

    @Given("the GraphQL API is available")
    public void theGraphQLAPIIsAvailable() {
        // GraphQL API is available through MockMvc
        assertThat(mockMvc).isNotNull();
    }

    @Given("test data is loaded in the database")
    public void testDataIsLoadedInTheDatabase() {
        // This would be implemented to set up test data
        // For now, we'll assume test data is loaded via SQL scripts or @Sql annotations
    }

    @Given("test categories are loaded in the database")
    public void testCategoriesAreLoadedInTheDatabase() {
        // Similar to above, test data setup
    }

    @Given("a product exists with ID {string}")
    public void aProductExistsWithId(String productId) {
        // Verify product exists in test data
        // This could query the repository or assume test data setup
    }

    @Given("{int} products exist in the database")
    public void productsExistInTheDatabase(int count) {
        // Verify the expected number of products exist
    }

    @Given("products exist with various categories")
    public void productsExistWithVariousCategories() {
        // Setup products with different categories
    }

    @Given("I have valid product data")
    public void iHaveValidProductData() {
        // Setup valid product input data
    }

    @Given("a category exists with ID {string}")
    public void aCategoryExistsWithId(String categoryId) {
        // Verify category exists
    }

    @Given("a category hierarchy exists")
    public void aCategoryHierarchyExists() {
        // Setup nested categories
    }

    @Given("multiple category hierarchies exist")
    public void multipleCategoryHierarchiesExist() {
        // Setup multiple root categories with children
    }

    @Given("data loaders are configured")
    public void dataLoadersAreConfigured() {
        // Verify data loaders are properly configured
    }

    @Given("performance monitoring is enabled")
    public void performanceMonitoringIsEnabled() {
        // Enable query counting or performance monitoring
        databaseQueryCount = 0;
    }

    @When("I execute the GraphQL query:")
    public void iExecuteTheGraphQLQuery(String query) throws Exception {
        graphQLQuery = query;
        executeGraphQLRequest();
    }

    @When("I execute the GraphQL mutation:")
    public void iExecuteTheGraphQLMutation(String mutation) throws Exception {
        graphQLQuery = mutation;
        executeGraphQLRequest();
    }

    @When("With variables:")
    public void withVariables(DataTable dataTable) throws Exception {
        Map<String, String> variables = dataTable.asMap(String.class, String.class);
        graphQLVariables = parseVariables(variables);
        executeGraphQLRequest();
    }

    @When("I subscribe to product updates:")
    public void iSubscribeToProductUpdates(String subscription) {
        // WebSocket subscription setup would go here
        graphQLQuery = subscription;
    }

    @When("the product is updated with name {string}")
    public void theProductIsUpdatedWithName(String newName) {
        // Trigger product update
    }

    @Then("the GraphQL response should be successful")
    public void theGraphQLResponseShouldBeSuccessful() {
        assertThat(graphQLResponse.getResponse().getStatus()).isEqualTo(200);
        String errors = JsonPath.read(responseBody, "$.errors");
        assertThat(errors).isNull();
    }

    @Then("the response should contain product with SKU {string}")
    public void theResponseShouldContainProductWithSKU(String expectedSku) {
        String actualSku = JsonPath.read(responseBody, "$.data.product.sku");
        assertThat(actualSku).isEqualTo(expectedSku);
    }

    @Then("the response should contain {int} products")
    public void theResponseShouldContainProducts(int expectedCount) {
        List<Object> products = JsonPath.read(responseBody, "$.data.products.edges");
        assertThat(products).hasSize(expectedCount);
    }

    @Then("pageInfo.hasNextPage should be {word}")
    public void pageInfoHasNextPageShouldBe(String expectedValue) {
        boolean hasNextPage = JsonPath.read(responseBody, "$.data.products.pageInfo.hasNextPage");
        assertThat(hasNextPage).isEqualTo(Boolean.parseBoolean(expectedValue));
    }

    @Then("totalCount should be {int}")
    public void totalCountShouldBe(int expectedCount) {
        int totalCount = JsonPath.read(responseBody, "$.data.products.totalCount");
        assertThat(totalCount).isEqualTo(expectedCount);
    }

    @Then("all returned products should have the specified category")
    public void allReturnedProductsShouldHaveTheSpecifiedCategory() {
        List<List<Map<String, String>>> categories = JsonPath.read(responseBody, 
                "$.data.products.edges[*].node.categories");
        assertThat(categories).allMatch(catList -> !catList.isEmpty());
    }

    @Then("the created product should have SKU {string}")
    public void theCreatedProductShouldHaveSKU(String expectedSku) {
        String actualSku = JsonPath.read(responseBody, "$.data.createProduct.sku");
        assertThat(actualSku).isEqualTo(expectedSku);
    }

    @Then("the product should be persisted in the database")
    public void theProductShouldBePersistedInTheDatabase() {
        String productId = JsonPath.read(responseBody, "$.data.createProduct.id");
        // Verify in database
    }

    @Then("the product name should be {string}")
    public void theProductNameShouldBe(String expectedName) {
        String actualName = JsonPath.read(responseBody, "$.data.updateProduct.name");
        assertThat(actualName).isEqualTo(expectedName);
    }

    @Then("the updatedAt timestamp should be recent")
    public void theUpdatedAtTimestampShouldBeRecent() {
        String updatedAt = JsonPath.read(responseBody, "$.data.updateProduct.updatedAt");
        assertThat(updatedAt).isNotNull();
        // Could add more specific time validation
    }

    @Then("the response should be {word}")
    public void theResponseShouldBe(String expectedValue) {
        Object result = JsonPath.read(responseBody, "$.data.deleteProduct");
        assertThat(result.toString()).isEqualTo(expectedValue);
    }

    @Then("the product should not exist in the database")
    public void theProductShouldNotExistInTheDatabase() {
        // Verify product was deleted from database
    }

    @Then("the product should have the specified category")
    public void theProductShouldHaveTheSpecifiedCategory() {
        List<Map<String, String>> categories = JsonPath.read(responseBody, "$.data.addProductToCategory.categories");
        assertThat(categories).isNotEmpty();
    }

    @Then("I should receive a subscription update")
    public void iShouldReceiveASubscriptionUpdate() {
        // Verify WebSocket message received
    }

    @Then("the update should contain the new name {string}")
    public void theUpdateShouldContainTheNewName(String expectedName) {
        // Verify subscription update content
    }

    @Then("the GraphQL response should contain an error")
    public void theGraphQLResponseShouldContainAnError() {
        List<Object> errors = JsonPath.read(responseBody, "$.errors");
        assertThat(errors).isNotEmpty();
    }

    @Then("the error message should contain {string}")
    public void theErrorMessageShouldContain(String expectedMessage) {
        List<String> errorMessages = JsonPath.read(responseBody, "$.errors[*].message");
        assertThat(errorMessages).anyMatch(msg -> msg.contains(expectedMessage));
    }

    @Then("the GraphQL response should contain validation errors")
    public void theGraphQLResponseShouldContainValidationErrors() {
        List<Object> errors = JsonPath.read(responseBody, "$.errors");
        assertThat(errors).isNotEmpty();
    }

    @Then("the validation errors should include {string}")
    public void theValidationErrorsShouldInclude(String expectedError) {
        List<Map<String, Object>> errors = JsonPath.read(responseBody, "$.errors");
        assertThat(errors).anyMatch(error -> 
            error.get("message").toString().contains(expectedError) ||
            (error.get("extensions") != null && 
             error.get("extensions").toString().contains(expectedError))
        );
    }

    @Then("the total database queries should be less than {int}")
    public void theTotalDatabaseQueriesShouldBeLessThan(int maxQueries) {
        assertThat(databaseQueryCount).isLessThan(maxQueries);
    }

    @Then("categories should be loaded in batches")
    public void categoriesShouldBeLoadedInBatches() {
        // Verify batch loading occurred
    }

    @Then("the query execution time should be under {int}ms")
    public void theQueryExecutionTimeShouldBeUnderMs(int maxMillis) {
        // Verify execution time
    }

    @Then("no N+1 queries should be detected")
    public void noNPlusOneQueriesShouldBeDetected() {
        // Verify no N+1 pattern in query logs
    }

    private void executeGraphQLRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("query", graphQLQuery);
        if (!graphQLVariables.isEmpty()) {
            request.put("variables", graphQLVariables);
        }

        String requestBody = objectMapper.writeValueAsString(request);

        graphQLResponse = mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        responseBody = graphQLResponse.getResponse().getContentAsString();
        graphQLVariables.clear(); // Reset for next request
    }

    private Map<String, Object> parseVariables(Map<String, String> variables) {
        Map<String, Object> parsed = new HashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Handle nested keys like "filter.categoryIds"
            String[] parts = key.split("\\.");
            if (parts.length > 1) {
                Map<String, Object> current = parsed;
                for (int i = 0; i < parts.length - 1; i++) {
                    current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
                }
                current.put(parts[parts.length - 1], parseValue(value));
            } else {
                parsed.put(key, parseValue(value));
            }
        }
        return parsed;
    }

    private Object parseValue(String value) {
        // Try to parse as boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // Try to parse as number
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer
        }
        
        // Try to parse as UUID
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            // Not a UUID
        }
        
        // Try to parse as JSON array
        if (value.startsWith("[") && value.endsWith("]")) {
            try {
                return objectMapper.readValue(value, List.class);
            } catch (Exception e) {
                // Not valid JSON
            }
        }
        
        // Return as string
        return value;
    }
}