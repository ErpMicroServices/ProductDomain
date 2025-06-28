package com.erpmicroservices.productdomain.api.graphql.dataloader;

import com.erpmicroservices.productdomain.database.repository.ProductRepository;
import com.erpmicroservices.productdomain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Batch loader for products to prevent N+1 queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBatchLoader implements BatchLoader<UUID, Product> {

    private final ProductRepository productRepository;

    @Override
    public CompletionStage<List<Product>> load(List<UUID> productIds) {
        log.debug("Batch loading products with IDs: {}", productIds);
        
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> uniqueIds = new HashSet<>(productIds);
            List<com.erpmicroservices.productdomain.database.entity.Product> entities = 
                productRepository.findAllById(uniqueIds);
            
            Map<UUID, Product> productMap = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toMap(Product::getId, p -> p));
            
            // Return in the same order as requested
            return productIds.stream()
                .map(productMap::get)
                .collect(Collectors.toList());
        });
    }
    
    private Product convertToDto(com.erpmicroservices.productdomain.database.entity.Product entity) {
        return Product.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .sku(entity.getSku())
            .price(entity.getPrice())
            .cost(entity.getCost())
            .active(entity.getActive())
            .status(entity.getStatus() != null ? entity.getStatus().name() : null)
            .weight(entity.getWeight())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}