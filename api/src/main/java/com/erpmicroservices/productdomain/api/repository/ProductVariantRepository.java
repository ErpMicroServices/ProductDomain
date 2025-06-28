package com.erpmicroservices.productdomain.api.repository;

import com.erpmicroservices.productdomain.database.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProductVariant entities.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>,
        JpaSpecificationExecutor<ProductVariant> {
    
    List<ProductVariant> findByProductId(UUID productId);
    
    Optional<ProductVariant> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    List<ProductVariant> findBySkuIn(List<String> skus);
}