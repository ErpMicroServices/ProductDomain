package com.erpmicroservices.productdomain.api.graphql.model.filter;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryFilter {
    private String name;
    private String slug;
    private UUID parentId;
    private Boolean isRoot;
}