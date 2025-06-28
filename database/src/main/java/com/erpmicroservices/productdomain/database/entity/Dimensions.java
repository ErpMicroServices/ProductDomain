package com.erpmicroservices.productdomain.database.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Embeddable class for product dimensions.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dimensions {

    @Column(name = "dimension_length", precision = 10, scale = 3)
    private BigDecimal length;

    @Column(name = "dimension_width", precision = 10, scale = 3)
    private BigDecimal width;

    @Column(name = "dimension_height", precision = 10, scale = 3)
    private BigDecimal height;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_unit", length = 20)
    private DimensionUnit unit;
}