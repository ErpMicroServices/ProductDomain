package com.erpmicroservices.productdomain.database.repository;

import com.erpmicroservices.productdomain.database.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    
    List<Category> findByParentId(UUID parentId);
    
    List<Category> findByParentIsNull();
    
    Optional<Category> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    boolean existsBySlugAndIdNot(String slug, UUID id);
    
    List<Category> findByNameContainingIgnoreCase(String name);
}