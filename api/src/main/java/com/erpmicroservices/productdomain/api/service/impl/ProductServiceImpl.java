package com.erpmicroservices.productdomain.api.service.impl;

import com.erpmicroservices.productdomain.api.graphql.model.filter.ProductFilter;
import com.erpmicroservices.productdomain.api.graphql.model.input.CreateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateProductInput;
import com.erpmicroservices.productdomain.api.repository.CategoryRepository;
import com.erpmicroservices.productdomain.api.repository.ProductRepository;
import com.erpmicroservices.productdomain.api.service.ProductService;
import com.erpmicroservices.productdomain.database.entity.Category;
import com.erpmicroservices.productdomain.database.entity.ProductStatus;
import com.erpmicroservices.productdomain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ProductService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    // Sinks for reactive subscriptions
    private final Sinks.Many<Product> productUpdatesSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Product> productCreationSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<UUID> productDeletionSink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return productRepository.findById(id)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAll(ProductFilter filter, Pageable pageable) {
        Specification<com.erpmicroservices.productdomain.database.entity.Product> spec = 
            buildSpecification(filter);
        return productRepository.findAll(spec, pageable)
                .map(this::convertToDto);
    }

    @Override
    public Product create(CreateProductInput input) {
        if (productRepository.existsBySku(input.getSku())) {
            throw new IllegalArgumentException("Product with SKU " + input.getSku() + " already exists");
        }

        com.erpmicroservices.productdomain.database.entity.Product entity = 
            com.erpmicroservices.productdomain.database.entity.Product.builder()
                .name(input.getName())
                .description(input.getDescription())
                .sku(input.getSku())
                .price(input.getPrice())
                .cost(input.getCost())
                .active(input.getActive() != null ? input.getActive() : true)
                .status(input.getStatus() != null ? ProductStatus.valueOf(input.getStatus()) : ProductStatus.ACTIVE)
                .weight(input.getWeight())
                .attributes(input.getAttributes())
                .build();

        if (input.getTags() != null) {
            entity.getTags().addAll(input.getTags());
        }

        com.erpmicroservices.productdomain.database.entity.Product saved = productRepository.save(entity);
        Product dto = convertToDto(saved);
        
        // Emit creation event
        productCreationSink.tryEmitNext(dto);
        
        return dto;
    }

    @Override
    public Product update(UUID id, UpdateProductInput input) {
        com.erpmicroservices.productdomain.database.entity.Product entity = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        if (input.getName() != null) {
            entity.setName(input.getName());
        }
        if (input.getDescription() != null) {
            entity.setDescription(input.getDescription());
        }
        if (input.getSku() != null && !entity.getSku().equals(input.getSku())) {
            if (productRepository.existsBySku(input.getSku())) {
                throw new IllegalArgumentException("Product with SKU " + input.getSku() + " already exists");
            }
            entity.setSku(input.getSku());
        }
        if (input.getPrice() != null) {
            entity.setPrice(input.getPrice());
        }
        if (input.getCost() != null) {
            entity.setCost(input.getCost());
        }
        if (input.getActive() != null) {
            entity.setActive(input.getActive());
        }
        if (input.getStatus() != null) {
            entity.setStatus(ProductStatus.valueOf(input.getStatus()));
        }
        if (input.getWeight() != null) {
            entity.setWeight(input.getWeight());
        }
        if (input.getAttributes() != null) {
            entity.setAttributes(input.getAttributes());
        }
        if (input.getTags() != null) {
            entity.getTags().clear();
            entity.getTags().addAll(input.getTags());
        }

        com.erpmicroservices.productdomain.database.entity.Product saved = productRepository.save(entity);
        Product dto = convertToDto(saved);
        
        // Emit update event
        productUpdatesSink.tryEmitNext(dto);
        
        return dto;
    }

    @Override
    public boolean delete(UUID id) {
        if (!productRepository.existsById(id)) {
            return false;
        }
        
        productRepository.deleteById(id);
        
        // Emit deletion event
        productDeletionSink.tryEmitNext(id);
        
        return true;
    }

    @Override
    public Product addToCategory(UUID productId, UUID categoryId) {
        com.erpmicroservices.productdomain.database.entity.Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        product.addCategory(category);
        com.erpmicroservices.productdomain.database.entity.Product saved = productRepository.save(product);
        
        return convertToDto(saved);
    }

    @Override
    public Product removeFromCategory(UUID productId, UUID categoryId) {
        com.erpmicroservices.productdomain.database.entity.Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        product.removeCategory(category);
        com.erpmicroservices.productdomain.database.entity.Product saved = productRepository.save(product);
        
        return convertToDto(saved);
    }

    @Override
    public Flux<Product> subscribeToProductUpdates(UUID productId) {
        return productUpdatesSink.asFlux()
                .filter(product -> product.getId().equals(productId));
    }

    @Override
    public Flux<Product> subscribeToProductCreation() {
        return productCreationSink.asFlux();
    }

    @Override
    public Flux<UUID> subscribeToProductDeletion() {
        return productDeletionSink.asFlux();
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
                .attributes(entity.getAttributes())
                .tags(entity.getTags())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private Specification<com.erpmicroservices.productdomain.database.entity.Product> 
            buildSpecification(ProductFilter filter) {
        if (filter == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        Specification<com.erpmicroservices.productdomain.database.entity.Product> spec = 
                (root, query, cb) -> cb.conjunction();

        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + filter.getSearchTerm().toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + filter.getSearchTerm().toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("sku")), "%" + filter.getSearchTerm().toLowerCase() + "%")
            ));
        }

        if (filter.getSkus() != null && !filter.getSkus().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("sku").in(filter.getSkus()));
        }

        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(
                    filter.getStatus().stream().map(ProductStatus::valueOf).toList()
            ));
        }

        if (filter.getCreatedAfter() != null) {
            spec = spec.and((root, query, cb) -> 
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter()));
        }

        if (filter.getCreatedBefore() != null) {
            spec = spec.and((root, query, cb) -> 
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore()));
        }

        return spec;
    }
}