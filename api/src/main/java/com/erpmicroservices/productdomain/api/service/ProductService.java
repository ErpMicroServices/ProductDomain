package com.erpmicroservices.productdomain.api.service;

import com.erpmicroservices.productdomain.api.graphql.model.filter.ProductFilter;
import com.erpmicroservices.productdomain.api.graphql.model.input.CreateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateProductInput;
import com.erpmicroservices.productdomain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for product operations.
 */
public interface ProductService {
    
    Optional<Product> findById(UUID id);
    
    Optional<Product> findBySku(String sku);
    
    Page<Product> findAll(ProductFilter filter, Pageable pageable);
    
    Product create(CreateProductInput input);
    
    Product update(UUID id, UpdateProductInput input);
    
    boolean delete(UUID id);
    
    Product addToCategory(UUID productId, UUID categoryId);
    
    Product removeFromCategory(UUID productId, UUID categoryId);
    
    Flux<Product> subscribeToProductUpdates(UUID productId);
    
    Flux<Product> subscribeToProductCreation();
    
    Flux<UUID> subscribeToProductDeletion();
}