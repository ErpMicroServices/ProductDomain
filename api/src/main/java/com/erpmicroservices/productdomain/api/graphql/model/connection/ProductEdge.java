package com.erpmicroservices.productdomain.api.graphql.model.connection;

import com.erpmicroservices.productdomain.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Edge in the product connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEdge {
    private Product node;
    private String cursor;
}