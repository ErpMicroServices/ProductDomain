package com.erpmicroservices.productdomain.api.service;

import com.erpmicroservices.productdomain.database.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryService {
    Optional<Category> findById(UUID id);
    
    Page<Category> findAll(Specification<Category> spec, Pageable pageable);
    
    List<Category> findByIds(List<UUID> ids);
    
    List<Category> findByParentId(UUID parentId);
    
    List<Category> findRootCategories();
    
    Category create(Category category);
    
    Category update(UUID id, Category category);
    
    void deleteById(UUID id);
    
    boolean existsById(UUID id);
    
    long count(Specification<Category> spec);
    
    List<Category> getCategoryPath(UUID categoryId);
    
    List<Category> findAllDescendants(UUID categoryId);
    
    Flux<Category> subscribeToCategoryUpdates(UUID categoryId);
}