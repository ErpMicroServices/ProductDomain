package com.erpmicroservices.productdomain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Product DTO for GraphQL API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private UUID id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal cost;
    private Boolean active;
    private String status;
    private BigDecimal weight;
    private Dimensions dimensions;
    private Map<String, Object> attributes;
    private Set<String> tags;
    private Set<Category> categories;
    private Category primaryCategory;
    private List<ProductVariant> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}