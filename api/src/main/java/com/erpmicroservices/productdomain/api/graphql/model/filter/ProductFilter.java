package com.erpmicroservices.productdomain.api.graphql.model.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filter for querying products.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilter {
    
    private String searchTerm;
    private List<String> skus;
    private List<String> status;
    private List<UUID> categoryIds;
    private List<String> tags;
    private PriceRange priceRange;
    private Boolean hasVariants;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
    }
}