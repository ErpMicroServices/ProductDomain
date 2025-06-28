package com.erpmicroservices.productdomain.api.graphql.model.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Input type for product dimensions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionsInput {
    
    @PositiveOrZero(message = "Length must be positive or zero")
    private BigDecimal length;
    
    @PositiveOrZero(message = "Width must be positive or zero")
    private BigDecimal width;
    
    @PositiveOrZero(message = "Height must be positive or zero")
    private BigDecimal height;
    
    private String unit;
}