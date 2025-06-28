package com.erpmicroservices.productdomain.api.graphql.model.input;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class UpdateCategoryInput {
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;
    
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 2, max = 100, message = "Category slug must be between 2 and 100 characters")
    private String slug;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private UUID parentId;
    
    private Map<String, Object> attributes;
}