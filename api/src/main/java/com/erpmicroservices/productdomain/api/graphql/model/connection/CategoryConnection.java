package com.erpmicroservices.productdomain.api.graphql.model.connection;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryConnection {
    private List<CategoryEdge> edges;
    private PageInfo pageInfo;
    private int totalCount;
}