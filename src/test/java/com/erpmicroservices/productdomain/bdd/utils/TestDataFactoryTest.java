package com.erpmicroservices.productdomain.bdd.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DisplayName("Test Data Factory Tests")
class TestDataFactoryTest {

    private TestDataFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TestDataFactory();
    }

    @Nested
    @DisplayName("Product Data Generation")
    class ProductDataTests {

        @Test
        @DisplayName("Should generate valid product with all required fields")
        void shouldGenerateValidProduct() {
            Map<String, Object> product = factory.createProduct();

            assertThat(product)
                .containsKeys("id", "name", "description", "sku", "price", "createdAt")
                .doesNotContainValue(null);

            assertThat(product.get("id")).isInstanceOf(UUID.class);
            assertThat(product.get("name")).isInstanceOf(String.class);
            assertThat(product.get("price")).isInstanceOf(BigDecimal.class);
            assertThat(product.get("createdAt")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("Should generate product with custom attributes")
        void shouldGenerateProductWithCustomAttributes() {
            Map<String, Object> customAttrs = Map.of(
                "name", "Custom Product",
                "price", new BigDecimal("99.99"),
                "category", "Electronics"
            );

            Map<String, Object> product = factory.createProduct(customAttrs);

            assertThat(product)
                .containsEntry("name", "Custom Product")
                .containsEntry("price", new BigDecimal("99.99"))
                .containsEntry("category", "Electronics");
        }

        @Test
        @DisplayName("Should generate unique SKUs for each product")
        void shouldGenerateUniqueSKUs() {
            List<Map<String, Object>> products = factory.createProducts(10);

            List<String> skus = products.stream()
                .map(p -> (String) p.get("sku"))
                .toList();

            assertThat(skus).hasSize(10);
            assertThat(skus).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should generate products with variants")
        void shouldGenerateProductsWithVariants() {
            Map<String, Object> product = factory.createProductWithVariants(3);

            assertThat(product).containsKey("variants");
            List<?> variants = (List<?>) product.get("variants");
            assertThat(variants).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Category Data Generation")
    class CategoryDataTests {

        @Test
        @DisplayName("Should generate valid category")
        void shouldGenerateValidCategory() {
            Map<String, Object> category = factory.createCategory();

            assertThat(category)
                .containsKeys("id", "name", "description", "parentId")
                .containsEntry("parentId", null);
        }

        @Test
        @DisplayName("Should generate category hierarchy")
        void shouldGenerateCategoryHierarchy() {
            Map<String, Object> hierarchy = factory.createCategoryHierarchy(3, 2);

            assertThat(hierarchy)
                .containsKey("children");
            
            List<?> children = (List<?>) hierarchy.get("children");
            assertThat(children).hasSize(3);
        }

        @Test
        @DisplayName("Should generate category with parent")
        void shouldGenerateCategoryWithParent() {
            UUID parentId = UUID.randomUUID();
            Map<String, Object> category = factory.createCategory(
                Map.of("parentId", parentId)
            );

            assertThat(category.get("parentId")).isEqualTo(parentId);
        }
    }

    @Nested
    @DisplayName("Inventory Data Generation")
    class InventoryDataTests {

        @Test
        @DisplayName("Should generate inventory record")
        void shouldGenerateInventoryRecord() {
            UUID productId = UUID.randomUUID();
            Map<String, Object> inventory = factory.createInventory(productId);

            assertThat(inventory)
                .containsKeys("productId", "quantity", "location", "lastUpdated")
                .containsEntry("productId", productId);

            assertThat(inventory.get("quantity")).isInstanceOf(Integer.class);
            assertThat((Integer) inventory.get("quantity")).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should generate inventory transaction")
        void shouldGenerateInventoryTransaction() {
            UUID productId = UUID.randomUUID();
            Map<String, Object> transaction = factory.createInventoryTransaction(
                productId, "INBOUND", 100
            );

            assertThat(transaction)
                .containsEntry("productId", productId)
                .containsEntry("type", "INBOUND")
                .containsEntry("quantity", 100)
                .containsKey("transactionDate");
        }
    }

    @Nested
    @DisplayName("Pricing Data Generation")
    class PricingDataTests {

        @Test
        @DisplayName("Should generate price record")
        void shouldGeneratePriceRecord() {
            UUID productId = UUID.randomUUID();
            Map<String, Object> price = factory.createPrice(productId);

            assertThat(price)
                .containsKeys("productId", "price", "currency", "effectiveDate")
                .containsEntry("productId", productId);

            assertThat(price.get("price")).isInstanceOf(BigDecimal.class);
            assertThat((BigDecimal) price.get("price")).isPositive();
        }

        @Test
        @DisplayName("Should generate price with discount")
        void shouldGeneratePriceWithDiscount() {
            UUID productId = UUID.randomUUID();
            Map<String, Object> price = factory.createPriceWithDiscount(
                productId, 
                new BigDecimal("100.00"),
                20 // 20% discount
            );

            assertThat(price)
                .containsEntry("originalPrice", new BigDecimal("100.00"))
                .containsEntry("discountPercentage", 20)
                .containsEntry("discountedPrice", new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("Should generate bulk pricing tiers")
        void shouldGenerateBulkPricingTiers() {
            UUID productId = UUID.randomUUID();
            List<Map<String, Object>> tiers = factory.createBulkPricingTiers(productId);

            assertThat(tiers).isNotEmpty();
            assertThat(tiers).allSatisfy(tier -> {
                assertThat(tier)
                    .containsKeys("minQuantity", "maxQuantity", "price");
            });
        }
    }

    @Nested
    @DisplayName("Test Data Sets")
    class TestDataSetsTests {

        @Test
        @DisplayName("Should generate complete product catalog")
        void shouldGenerateCompleteProductCatalog() {
            Map<String, Object> catalog = factory.createProductCatalog(
                5,  // categories
                20  // products
            );

            assertThat(catalog)
                .containsKeys("categories", "products", "categoryProducts");

            List<?> categories = (List<?>) catalog.get("categories");
            List<?> products = (List<?>) catalog.get("products");
            
            assertThat(categories).hasSize(5);
            assertThat(products).hasSize(20);
        }

        @Test
        @DisplayName("Should generate test scenario data")
        void shouldGenerateTestScenarioData() {
            Map<String, Object> scenario = factory.createScenarioData("product-search");

            assertThat(scenario)
                .containsKeys("scenarioName", "testData", "expectedResults");
        }
    }

    @Nested
    @DisplayName("Random Data Generation")
    class RandomDataTests {

        @Test
        @DisplayName("Should generate random strings of specified length")
        void shouldGenerateRandomStrings() {
            String random = factory.randomString(10);
            
            assertThat(random).hasSize(10);
            assertThat(random).matches("[A-Za-z0-9]+");
        }

        @Test
        @DisplayName("Should generate random numbers in range")
        void shouldGenerateRandomNumbersInRange() {
            for (int i = 0; i < 100; i++) {
                int random = factory.randomInt(10, 20);
                assertThat(random).isBetween(10, 20);
            }
        }

        @Test
        @DisplayName("Should generate random email addresses")
        void shouldGenerateRandomEmails() {
            String email = factory.randomEmail();
            
            assertThat(email).matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        }

        @Test
        @DisplayName("Should generate random phone numbers")
        void shouldGenerateRandomPhoneNumbers() {
            String phone = factory.randomPhone();
            
            assertThat(phone).matches("\\+?[0-9]{10,15}");
        }
    }

    @Nested
    @DisplayName("Data Relationship Tests")
    class DataRelationshipTests {

        @Test
        @DisplayName("Should maintain referential integrity")
        void shouldMaintainReferentialIntegrity() {
            Map<String, Object> data = factory.createRelatedData();

            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("products");
            List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
            
            // Verify all product category IDs exist
            products.forEach(product -> {
                UUID categoryId = (UUID) product.get("categoryId");
                if (categoryId != null) {
                    boolean exists = categories.stream()
                        .anyMatch(cat -> cat.get("id").equals(categoryId));
                    assertThat(exists).isTrue();
                }
            });
        }

        @Test
        @DisplayName("Should generate consistent data across related entities")
        void shouldGenerateConsistentData() {
            Map<String, Object> order = factory.createOrderWithItems(5);

            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            BigDecimal orderTotal = (BigDecimal) order.get("total");
            
            BigDecimal calculatedTotal = items.stream()
                .map(item -> {
                    BigDecimal price = (BigDecimal) item.get("price");
                    Integer quantity = (Integer) item.get("quantity");
                    return price.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            assertThat(orderTotal).isEqualByComparingTo(calculatedTotal);
        }
    }
}