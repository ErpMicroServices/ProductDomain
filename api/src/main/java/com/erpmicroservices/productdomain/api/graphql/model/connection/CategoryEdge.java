package com.erpmicroservices.productdomain.api.graphql.model.connection;

import com.erpmicroservices.productdomain.database.entity.Category;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryEdge {
    private Category node;
    private String cursor;
}