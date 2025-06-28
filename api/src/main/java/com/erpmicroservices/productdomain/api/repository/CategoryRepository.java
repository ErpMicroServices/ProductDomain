package com.erpmicroservices.productdomain.api.repository;

import com.erpmicroservices.productdomain.database.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Category entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    
    List<Category> findByParentIsNull();
    
    List<Category> findByParentId(UUID parentId);
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.sortOrder, c.name")
    List<Category> findAllActive();
    
    @Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c.id = :categoryId")
    Long countProductsByCategoryId(UUID categoryId);
}