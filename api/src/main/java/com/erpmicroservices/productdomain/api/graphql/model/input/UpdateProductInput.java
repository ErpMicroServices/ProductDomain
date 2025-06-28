package com.erpmicroservices.productdomain.api.graphql.model.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Input type for updating an existing product.
 * All fields are optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductInput {
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;
    
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;
    
    @PositiveOrZero(message = "Price must be positive or zero")
    private BigDecimal price;
    
    @PositiveOrZero(message = "Cost must be positive or zero")
    private BigDecimal cost;
    
    private Boolean active;
    
    private String status;
    
    @PositiveOrZero(message = "Weight must be positive or zero")
    private BigDecimal weight;
    
    private DimensionsInput dimensions;
    
    private Map<String, Object> attributes;
    
    private List<String> tags;
}