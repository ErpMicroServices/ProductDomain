package com.erpmicroservices.productdomain.api.graphql.dataloader;

import com.erpmicroservices.productdomain.api.repository.CategoryRepository;
import com.erpmicroservices.productdomain.model.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Batch loader for categories to prevent N+1 queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryBatchLoader implements BatchLoader<UUID, Category> {

    private final CategoryRepository categoryRepository;

    @Override
    public CompletionStage<List<Category>> load(List<UUID> categoryIds) {
        log.debug("Batch loading categories with IDs: {}", categoryIds);
        
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> uniqueIds = new HashSet<>(categoryIds);
            List<com.erpmicroservices.productdomain.database.entity.Category> entities = 
                categoryRepository.findAllById(uniqueIds);
            
            Map<UUID, Category> categoryMap = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toMap(Category::getId, c -> c));
            
            // Return in the same order as requested
            return categoryIds.stream()
                .map(categoryMap::get)
                .collect(Collectors.toList());
        });
    }
    
    private Category convertToDto(com.erpmicroservices.productdomain.database.entity.Category entity) {
        return Category.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
            .path(entity.getPath())
            .level(entity.getLevel())
            .sortOrder(entity.getSortOrder())
            .isActive(entity.getIsActive())
            .imageUrl(entity.getImageUrl())
            .attributes(entity.getAttributes())
            .childrenCount(entity.getChildrenCount())
            .productCount(entity.getProductCount())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}