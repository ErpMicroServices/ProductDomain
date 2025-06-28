package com.erpmicroservices.productdomain.api.service.impl;

import com.erpmicroservices.productdomain.api.service.CategoryService;
import com.erpmicroservices.productdomain.database.entity.Category;
import com.erpmicroservices.productdomain.database.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final Sinks.Many<Category> categoryUpdatesSink = Sinks.many().multicast().onBackpressureBuffer();
    
    @Override
    @Cacheable(value = "categories", key = "#id")
    public Optional<Category> findById(UUID id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }
    
    @Override
    public Page<Category> findAll(Specification<Category> spec, Pageable pageable) {
        log.debug("Finding all categories with specification and pagination");
        return categoryRepository.findAll(spec, pageable);
    }
    
    @Override
    public List<Category> findByIds(List<UUID> ids) {
        log.debug("Finding categories by IDs: {}", ids);
        return categoryRepository.findAllById(ids);
    }
    
    @Override
    public List<Category> findByParentId(UUID parentId) {
        log.debug("Finding categories by parent ID: {}", parentId);
        return categoryRepository.findByParentId(parentId);
    }
    
    @Override
    public List<Category> findRootCategories() {
        log.debug("Finding root categories");
        return categoryRepository.findByParentIsNull();
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(Category category) {
        log.info("Creating category: {}", category.getName());
        
        // Generate slug if not provided
        if (category.getSlug() == null || category.getSlug().isEmpty()) {
            category.setSlug(generateSlug(category.getName()));
        }
        
        // Ensure unique slug
        String baseSlug = category.getSlug();
        int counter = 1;
        while (categoryRepository.existsBySlug(category.getSlug())) {
            category.setSlug(baseSlug + "-" + counter);
            counter++;
        }
        
        Category saved = categoryRepository.save(category);
        categoryUpdatesSink.tryEmitNext(saved);
        return saved;
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(UUID id, Category category) {
        log.info("Updating category ID: {}", id);
        
        category.setId(id);
        
        // Ensure slug uniqueness if changed
        Optional<Category> existing = categoryRepository.findById(id);
        if (existing.isPresent() && !existing.get().getSlug().equals(category.getSlug())) {
            if (categoryRepository.existsBySlugAndIdNot(category.getSlug(), id)) {
                throw new IllegalArgumentException("Slug already exists: " + category.getSlug());
            }
        }
        
        Category saved = categoryRepository.save(category);
        categoryUpdatesSink.tryEmitNext(saved);
        return saved;
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteById(UUID id) {
        log.info("Deleting category ID: {}", id);
        
        // Check for child categories
        if (!categoryRepository.findByParentId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete category with child categories");
        }
        
        categoryRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return categoryRepository.existsById(id);
    }
    
    @Override
    public long count(Specification<Category> spec) {
        return categoryRepository.count(spec);
    }
    
    @Override
    public List<Category> getCategoryPath(UUID categoryId) {
        log.debug("Getting category path for ID: {}", categoryId);
        
        List<Category> path = new ArrayList<>();
        Optional<Category> current = categoryRepository.findById(categoryId);
        
        while (current.isPresent()) {
            path.add(0, current.get());
            current = current.get().getParent() != null 
                ? categoryRepository.findById(current.get().getParent().getId())
                : Optional.empty();
        }
        
        return path;
    }
    
    @Override
    public List<Category> findAllDescendants(UUID categoryId) {
        log.debug("Finding all descendants for category ID: {}", categoryId);
        
        List<Category> descendants = new ArrayList<>();
        Queue<UUID> queue = new LinkedList<>();
        queue.add(categoryId);
        
        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            List<Category> children = categoryRepository.findByParentId(currentId);
            descendants.addAll(children);
            children.forEach(child -> queue.add(child.getId()));
        }
        
        return descendants;
    }
    
    @Override
    public Flux<Category> subscribeToCategoryUpdates(UUID categoryId) {
        return categoryUpdatesSink.asFlux()
                .filter(category -> category.getId().equals(categoryId));
    }
    
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}