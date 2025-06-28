package com.erpmicroservices.productdomain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * ProductVariant DTO for GraphQL API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    private UUID id;
    private Product product;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal cost;
    private BigDecimal weight;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Boolean isActive;
    private Boolean isDefault;
    private Map<String, Object> attributes;
    private Dimensions dimensions;
    private String barcode;
    private String upc;
    private String isbn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}