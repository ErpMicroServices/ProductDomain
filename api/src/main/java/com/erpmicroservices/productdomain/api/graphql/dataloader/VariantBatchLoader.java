package com.erpmicroservices.productdomain.api.graphql.dataloader;

import com.erpmicroservices.productdomain.api.repository.ProductVariantRepository;
import com.erpmicroservices.productdomain.model.ProductVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Batch loader for product variants to prevent N+1 queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantBatchLoader implements BatchLoader<UUID, ProductVariant> {

    private final ProductVariantRepository variantRepository;

    @Override
    public CompletionStage<List<ProductVariant>> load(List<UUID> variantIds) {
        log.debug("Batch loading variants with IDs: {}", variantIds);
        
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> uniqueIds = new HashSet<>(variantIds);
            List<com.erpmicroservices.productdomain.database.entity.ProductVariant> entities = 
                variantRepository.findAllById(uniqueIds);
            
            Map<UUID, ProductVariant> variantMap = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
            
            // Return in the same order as requested
            return variantIds.stream()
                .map(variantMap::get)
                .collect(Collectors.toList());
        });
    }
    
    private ProductVariant convertToDto(com.erpmicroservices.productdomain.database.entity.ProductVariant entity) {
        return ProductVariant.builder()
            .id(entity.getId())
            .name(entity.getName())
            .sku(entity.getSku())
            .price(entity.getPrice())
            .cost(entity.getCost())
            .weight(entity.getWeight())
            .stockQuantity(entity.getStockQuantity())
            .reservedQuantity(entity.getReservedQuantity())
            .availableQuantity(entity.getAvailableQuantity())
            .reorderLevel(entity.getReorderLevel())
            .maxStockLevel(entity.getMaxStockLevel())
            .isActive(entity.getIsActive())
            .isDefault(entity.getIsDefault())
            .attributes(entity.getAttributes())
            .barcode(entity.getBarcode())
            .upc(entity.getUpc())
            .isbn(entity.getIsbn())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}