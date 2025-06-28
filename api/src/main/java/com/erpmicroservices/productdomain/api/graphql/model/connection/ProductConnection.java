package com.erpmicroservices.productdomain.api.graphql.model.connection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Relay-style connection for products.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConnection {
    private List<ProductEdge> edges;
    private PageInfo pageInfo;
    private int totalCount;
}