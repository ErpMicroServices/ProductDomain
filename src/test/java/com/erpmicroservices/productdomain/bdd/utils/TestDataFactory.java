package com.erpmicroservices.productdomain.bdd.utils;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Factory class for generating test data for BDD scenarios.
 * Provides methods for creating consistent, realistic test data.
 */
@Component
public class TestDataFactory {

    private final Faker faker = new Faker();
    private final Random random = new Random();
    
    // Product-related data generation
    
    public Map<String, Object> createProduct() {
        return createProduct(Collections.emptyMap());
    }
    
    public Map<String, Object> createProduct(Map<String, Object> overrides) {
        Map<String, Object> product = new HashMap<>();
        
        product.put("id", UUID.randomUUID());
        product.put("name", faker.commerce().productName());
        product.put("description", faker.lorem().paragraph());
        product.put("sku", generateSKU());
        product.put("price", randomPrice(10, 1000));
        product.put("cost", randomPrice(5, 500));
        product.put("weight", randomDouble(0.1, 50.0));
        product.put("dimensions", generateDimensions());
        product.put("active", true);
        product.put("createdAt", LocalDateTime.now());
        product.put("updatedAt", LocalDateTime.now());
        
        // Apply overrides
        product.putAll(overrides);
        
        return product;
    }
    
    public List<Map<String, Object>> createProducts(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createProduct())
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> createProductWithVariants(int variantCount) {
        Map<String, Object> product = createProduct();
        List<Map<String, Object>> variants = IntStream.range(0, variantCount)
            .mapToObj(i -> createProductVariant((UUID) product.get("id"), i))
            .collect(Collectors.toList());
        
        product.put("variants", variants);
        return product;
    }
    
    private Map<String, Object> createProductVariant(UUID productId, int index) {
        Map<String, Object> variant = new HashMap<>();
        
        variant.put("id", UUID.randomUUID());
        variant.put("productId", productId);
        variant.put("sku", generateSKU() + "-V" + (index + 1));
        variant.put("name", "Variant " + (index + 1));
        variant.put("attributes", generateVariantAttributes());
        variant.put("price", randomPrice(10, 1000));
        variant.put("stock", randomInt(0, 1000));
        
        return variant;
    }
    
    // Category-related data generation
    
    public Map<String, Object> createCategory() {
        return createCategory(Collections.emptyMap());
    }
    
    public Map<String, Object> createCategory(Map<String, Object> overrides) {
        Map<String, Object> category = new HashMap<>();
        
        category.put("id", UUID.randomUUID());
        category.put("name", faker.commerce().department());
        category.put("description", faker.lorem().sentence());
        category.put("slug", faker.internet().slug());
        category.put("parentId", null);
        category.put("displayOrder", randomInt(1, 100));
        category.put("active", true);
        category.put("createdAt", LocalDateTime.now());
        
        category.putAll(overrides);
        
        return category;
    }
    
    public Map<String, Object> createCategoryHierarchy(int depth, int childrenPerLevel) {
        return createCategoryNode(null, depth, childrenPerLevel);
    }
    
    private Map<String, Object> createCategoryNode(UUID parentId, int remainingDepth, int childrenPerLevel) {
        Map<String, Object> category = createCategory(
            parentId != null ? Map.of("parentId", parentId) : Collections.emptyMap()
        );
        
        if (remainingDepth > 0) {
            List<Map<String, Object>> children = IntStream.range(0, childrenPerLevel)
                .mapToObj(i -> createCategoryNode((UUID) category.get("id"), remainingDepth - 1, childrenPerLevel))
                .collect(Collectors.toList());
            category.put("children", children);
        }
        
        return category;
    }
    
    // Inventory-related data generation
    
    public Map<String, Object> createInventory(UUID productId) {
        Map<String, Object> inventory = new HashMap<>();
        
        inventory.put("id", UUID.randomUUID());
        inventory.put("productId", productId);
        inventory.put("quantity", randomInt(0, 1000));
        inventory.put("reservedQuantity", randomInt(0, 50));
        inventory.put("location", faker.address().city());
        inventory.put("warehouse", faker.company().name() + " Warehouse");
        inventory.put("lastUpdated", LocalDateTime.now());
        inventory.put("reorderPoint", randomInt(10, 100));
        inventory.put("reorderQuantity", randomInt(50, 500));
        
        return inventory;
    }
    
    public Map<String, Object> createInventoryTransaction(UUID productId, String type, int quantity) {
        Map<String, Object> transaction = new HashMap<>();
        
        transaction.put("id", UUID.randomUUID());
        transaction.put("productId", productId);
        transaction.put("type", type); // INBOUND, OUTBOUND, ADJUSTMENT
        transaction.put("quantity", quantity);
        transaction.put("reason", faker.lorem().sentence());
        transaction.put("reference", "REF-" + faker.number().digits(8));
        transaction.put("transactionDate", LocalDateTime.now());
        transaction.put("performedBy", faker.name().fullName());
        
        return transaction;
    }
    
    // Pricing-related data generation
    
    public Map<String, Object> createPrice(UUID productId) {
        Map<String, Object> price = new HashMap<>();
        
        price.put("id", UUID.randomUUID());
        price.put("productId", productId);
        price.put("price", randomPrice(10, 1000));
        price.put("currency", "USD");
        price.put("effectiveDate", LocalDateTime.now());
        price.put("expiryDate", LocalDateTime.now().plusMonths(6));
        price.put("priceType", "REGULAR");
        
        return price;
    }
    
    public Map<String, Object> createPriceWithDiscount(UUID productId, BigDecimal originalPrice, int discountPercentage) {
        Map<String, Object> price = new HashMap<>();
        
        BigDecimal discountAmount = originalPrice.multiply(BigDecimal.valueOf(discountPercentage))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discountedPrice = originalPrice.subtract(discountAmount);
        
        price.put("id", UUID.randomUUID());
        price.put("productId", productId);
        price.put("originalPrice", originalPrice);
        price.put("discountPercentage", discountPercentage);
        price.put("discountAmount", discountAmount);
        price.put("discountedPrice", discountedPrice);
        price.put("currency", "USD");
        price.put("effectiveDate", LocalDateTime.now());
        price.put("expiryDate", LocalDateTime.now().plusDays(30));
        price.put("priceType", "PROMOTIONAL");
        
        return price;
    }
    
    public List<Map<String, Object>> createBulkPricingTiers(UUID productId) {
        List<Map<String, Object>> tiers = new ArrayList<>();
        
        int[] quantities = {10, 50, 100, 500};
        BigDecimal basePrice = randomPrice(10, 100);
        
        for (int i = 0; i < quantities.length; i++) {
            Map<String, Object> tier = new HashMap<>();
            
            tier.put("id", UUID.randomUUID());
            tier.put("productId", productId);
            tier.put("minQuantity", i == 0 ? 1 : quantities[i - 1]);
            tier.put("maxQuantity", quantities[i] - 1);
            tier.put("price", basePrice.multiply(BigDecimal.valueOf(1 - (i * 0.05))));
            tier.put("tierLevel", i + 1);
            
            tiers.add(tier);
        }
        
        return tiers;
    }
    
    // Order-related data generation
    
    public Map<String, Object> createOrderWithItems(int itemCount) {
        Map<String, Object> order = new HashMap<>();
        
        order.put("id", UUID.randomUUID());
        order.put("orderNumber", "ORD-" + faker.number().digits(8));
        order.put("customerId", UUID.randomUUID());
        order.put("status", "PENDING");
        order.put("orderDate", LocalDateTime.now());
        
        List<Map<String, Object>> items = IntStream.range(0, itemCount)
            .mapToObj(i -> createOrderItem((UUID) order.get("id")))
            .collect(Collectors.toList());
        
        BigDecimal subtotal = items.stream()
            .map(item -> ((BigDecimal) item.get("price")).multiply(BigDecimal.valueOf((Integer) item.get("quantity"))))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);
        
        order.put("items", items);
        order.put("subtotal", subtotal);
        order.put("tax", tax);
        order.put("total", total);
        
        return order;
    }
    
    private Map<String, Object> createOrderItem(UUID orderId) {
        Map<String, Object> item = new HashMap<>();
        
        item.put("id", UUID.randomUUID());
        item.put("orderId", orderId);
        item.put("productId", UUID.randomUUID());
        item.put("productName", faker.commerce().productName());
        item.put("quantity", randomInt(1, 10));
        item.put("price", randomPrice(10, 500));
        
        return item;
    }
    
    // Test data sets
    
    public Map<String, Object> createProductCatalog(int categoryCount, int productCount) {
        List<Map<String, Object>> categories = IntStream.range(0, categoryCount)
            .mapToObj(i -> createCategory())
            .collect(Collectors.toList());
        
        List<Map<String, Object>> products = IntStream.range(0, productCount)
            .mapToObj(i -> {
                Map<String, Object> product = createProduct();
                // Assign random category
                if (!categories.isEmpty()) {
                    Map<String, Object> category = categories.get(random.nextInt(categories.size()));
                    product.put("categoryId", category.get("id"));
                }
                return product;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> catalog = new HashMap<>();
        catalog.put("categories", categories);
        catalog.put("products", products);
        catalog.put("categoryProducts", createCategoryProductMapping(categories, products));
        
        return catalog;
    }
    
    private Map<UUID, List<UUID>> createCategoryProductMapping(
            List<Map<String, Object>> categories,
            List<Map<String, Object>> products) {
        
        Map<UUID, List<UUID>> mapping = new HashMap<>();
        
        for (Map<String, Object> product : products) {
            UUID categoryId = (UUID) product.get("categoryId");
            if (categoryId != null) {
                mapping.computeIfAbsent(categoryId, k -> new ArrayList<>())
                    .add((UUID) product.get("id"));
            }
        }
        
        return mapping;
    }
    
    public Map<String, Object> createScenarioData(String scenarioName) {
        Map<String, Object> data = new HashMap<>();
        data.put("scenarioName", scenarioName);
        data.put("timestamp", LocalDateTime.now());
        
        switch (scenarioName) {
            case "product-search":
                data.put("testData", createProductSearchData());
                data.put("expectedResults", createExpectedSearchResults());
                break;
            case "inventory-update":
                data.put("testData", createInventoryUpdateData());
                break;
            case "pricing-rules":
                data.put("testData", createPricingRulesData());
                break;
            default:
                data.put("testData", Collections.emptyMap());
        }
        
        return data;
    }
    
    private Map<String, Object> createProductSearchData() {
        Map<String, Object> searchData = new HashMap<>();
        searchData.put("searchTerm", faker.commerce().productName());
        searchData.put("filters", Map.of(
            "minPrice", randomPrice(10, 50),
            "maxPrice", randomPrice(100, 500),
            "category", faker.commerce().department()
        ));
        searchData.put("products", createProducts(20));
        return searchData;
    }
    
    private Map<String, Object> createExpectedSearchResults() {
        Map<String, Object> results = new HashMap<>();
        results.put("totalCount", randomInt(5, 15));
        results.put("pageSize", 10);
        results.put("currentPage", 1);
        return results;
    }
    
    private Map<String, Object> createInventoryUpdateData() {
        Map<String, Object> data = new HashMap<>();
        UUID productId = UUID.randomUUID();
        
        data.put("product", createProduct(Map.of("id", productId)));
        data.put("currentInventory", createInventory(productId));
        data.put("transactions", IntStream.range(0, 5)
            .mapToObj(i -> createInventoryTransaction(productId, 
                i % 2 == 0 ? "INBOUND" : "OUTBOUND", 
                randomInt(10, 100)))
            .collect(Collectors.toList()));
        
        return data;
    }
    
    private Map<String, Object> createPricingRulesData() {
        Map<String, Object> data = new HashMap<>();
        UUID productId = UUID.randomUUID();
        
        data.put("product", createProduct(Map.of("id", productId)));
        data.put("regularPrice", createPrice(productId));
        data.put("bulkPricing", createBulkPricingTiers(productId));
        data.put("promotions", Arrays.asList(
            createPriceWithDiscount(productId, randomPrice(100, 200), 10),
            createPriceWithDiscount(productId, randomPrice(100, 200), 20)
        ));
        
        return data;
    }
    
    public Map<String, Object> createRelatedData() {
        Map<String, Object> catalog = createProductCatalog(5, 20);
        
        // Add inventory for products
        List<Map<String, Object>> products = (List<Map<String, Object>>) catalog.get("products");
        List<Map<String, Object>> inventory = products.stream()
            .map(p -> createInventory((UUID) p.get("id")))
            .collect(Collectors.toList());
        
        catalog.put("inventory", inventory);
        
        return catalog;
    }
    
    public Map<String, Object> loadScenarioData(String dataSet) {
        // This would typically load from files or database
        // For now, generate dynamically
        return createScenarioData(dataSet);
    }
    
    // Utility methods
    
    public String randomString(int length) {
        return faker.regexify("[A-Za-z0-9]{" + length + "}");
    }
    
    public int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    public double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    public BigDecimal randomPrice(double min, double max) {
        double price = randomDouble(min, max);
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }
    
    public String randomEmail() {
        return faker.internet().emailAddress();
    }
    
    public String randomPhone() {
        return faker.phoneNumber().phoneNumber();
    }
    
    private String generateSKU() {
        return faker.regexify("[A-Z]{3}-[0-9]{4}-[A-Z0-9]{4}");
    }
    
    private Map<String, Object> generateDimensions() {
        Map<String, Object> dimensions = new HashMap<>();
        dimensions.put("length", randomDouble(1, 100));
        dimensions.put("width", randomDouble(1, 100));
        dimensions.put("height", randomDouble(1, 100));
        dimensions.put("unit", "cm");
        return dimensions;
    }
    
    private Map<String, String> generateVariantAttributes() {
        Map<String, String> attributes = new HashMap<>();
        
        // Random attributes
        if (random.nextBoolean()) {
            attributes.put("color", faker.color().name());
        }
        if (random.nextBoolean()) {
            attributes.put("size", faker.options().option("XS", "S", "M", "L", "XL", "XXL"));
        }
        if (random.nextBoolean()) {
            attributes.put("material", faker.options().option("Cotton", "Polyester", "Wool", "Silk"));
        }
        
        return attributes;
    }
}