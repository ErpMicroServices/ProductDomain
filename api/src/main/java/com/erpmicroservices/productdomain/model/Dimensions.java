package com.erpmicroservices.productdomain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dimensions DTO for GraphQL API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dimensions {
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private String unit;
}