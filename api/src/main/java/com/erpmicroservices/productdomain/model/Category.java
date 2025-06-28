package com.erpmicroservices.productdomain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Category DTO for GraphQL API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private UUID id;
    private String name;
    private String description;
    private UUID parentId;
    private Category parent;
    private Set<Category> children;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private Boolean isActive;
    private String imageUrl;
    private Map<String, Object> attributes;
    private Integer childrenCount;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}