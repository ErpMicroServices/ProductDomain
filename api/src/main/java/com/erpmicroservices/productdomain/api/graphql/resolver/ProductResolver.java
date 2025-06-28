package com.erpmicroservices.productdomain.api.graphql.resolver;

import com.erpmicroservices.productdomain.api.graphql.model.connection.ProductConnection;
import com.erpmicroservices.productdomain.api.graphql.model.connection.ProductEdge;
import com.erpmicroservices.productdomain.api.graphql.model.filter.ProductFilter;
import com.erpmicroservices.productdomain.api.graphql.model.input.CreateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.connection.PageInfo;
import com.erpmicroservices.productdomain.api.service.ProductService;
import com.erpmicroservices.productdomain.model.Product;
import graphql.GraphQLException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for Product operations.
 * Handles queries, mutations, and subscriptions for products.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Validated
public class ProductResolver {

    private final ProductService productService;

    @QueryMapping
    public Product product(@Argument @NotNull UUID id) {
        log.debug("Fetching product with ID: {}", id);
        return productService.findById(id)
                .orElseThrow(() -> new GraphQLException("Product not found with ID: " + id));
    }

    @QueryMapping
    public ProductConnection products(@Argument Integer first,
                                    @Argument String after,
                                    @Argument ProductFilter filter,
                                    @Argument List<SortInput> sort) {
        log.debug("Fetching products with first: {}, after: {}, filter: {}", first, after, filter);
        
        int pageSize = first != null ? first : 20;
        int pageNumber = 0;
        
        if (after != null) {
            pageNumber = decodePageNumber(after);
        }
        
        Sort sortBy = createSort(sort);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, sortBy);
        
        Page<Product> productPage = productService.findAll(filter, pageRequest);
        
        return buildProductConnection(productPage);
    }

    @QueryMapping
    public Product productBySku(@Argument @NotNull String sku) {
        log.debug("Fetching product with SKU: {}", sku);
        return productService.findBySku(sku)
                .orElseThrow(() -> new GraphQLException("Product not found with SKU: " + sku));
    }

    @MutationMapping
    public Product createProduct(@Argument @Valid CreateProductInput input) {
        log.debug("Creating product with input: {}", input);
        
        try {
            validateCreateInput(input);
            return productService.create(input);
        } catch (Exception e) {
            log.error("Error creating product", e);
            throw new GraphQLException("Failed to create product: " + e.getMessage());
        }
    }

    @MutationMapping
    public Product updateProduct(@Argument @NotNull UUID id, 
                               @Argument @Valid UpdateProductInput input) {
        log.debug("Updating product {} with input: {}", id, input);
        
        try {
            return productService.update(id, input);
        } catch (Exception e) {
            log.error("Error updating product", e);
            throw new GraphQLException("Failed to update product: " + e.getMessage());
        }
    }

    @MutationMapping
    public Boolean deleteProduct(@Argument @NotNull UUID id) {
        log.debug("Deleting product with ID: {}", id);
        
        try {
            return productService.delete(id);
        } catch (Exception e) {
            log.error("Error deleting product", e);
            throw new GraphQLException("Failed to delete product: " + e.getMessage());
        }
    }

    @MutationMapping
    public Product addProductToCategory(@Argument @NotNull UUID productId, 
                                      @Argument @NotNull UUID categoryId) {
        log.debug("Adding product {} to category {}", productId, categoryId);
        
        try {
            return productService.addToCategory(productId, categoryId);
        } catch (Exception e) {
            log.error("Error adding product to category", e);
            throw new GraphQLException("Failed to add product to category: " + e.getMessage());
        }
    }

    @MutationMapping
    public Product removeProductFromCategory(@Argument @NotNull UUID productId, 
                                           @Argument @NotNull UUID categoryId) {
        log.debug("Removing product {} from category {}", productId, categoryId);
        
        try {
            return productService.removeFromCategory(productId, categoryId);
        } catch (Exception e) {
            log.error("Error removing product from category", e);
            throw new GraphQLException("Failed to remove product from category: " + e.getMessage());
        }
    }

    @SubscriptionMapping
    public Flux<Product> productUpdated(@Argument @NotNull UUID productId) {
        log.debug("Subscribing to updates for product: {}", productId);
        return productService.subscribeToProductUpdates(productId);
    }

    @SubscriptionMapping
    public Flux<Product> productCreated() {
        log.debug("Subscribing to product creation events");
        return productService.subscribeToProductCreation();
    }

    @SubscriptionMapping
    public Flux<UUID> productDeleted() {
        log.debug("Subscribing to product deletion events");
        return productService.subscribeToProductDeletion();
    }

    private void validateCreateInput(CreateProductInput input) {
        if (input.getSku() == null || input.getSku().trim().isEmpty()) {
            throw new GraphQLException("SKU is required");
        }
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            throw new GraphQLException("Name is required");
        }
    }

    private ProductConnection buildProductConnection(Page<Product> page) {
        List<ProductEdge> edges = page.getContent().stream()
                .map(product -> ProductEdge.builder()
                        .node(product)
                        .cursor(encodeCursor(page.getNumber(), product.getId()))
                        .build())
                .collect(Collectors.toList());

        PageInfo pageInfo = PageInfo.builder()
                .hasNextPage(page.hasNext())
                .hasPreviousPage(page.hasPrevious())
                .startCursor(edges.isEmpty() ? null : edges.get(0).getCursor())
                .endCursor(edges.isEmpty() ? null : edges.get(edges.size() - 1).getCursor())
                .build();

        return ProductConnection.builder()
                .edges(edges)
                .pageInfo(pageInfo)
                .totalCount((int) page.getTotalElements())
                .build();
    }

    private String encodeCursor(int pageNumber, UUID id) {
        String cursor = pageNumber + ":" + id.toString();
        return Base64.getEncoder().encodeToString(cursor.getBytes());
    }

    private int decodePageNumber(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            return Integer.parseInt(decoded.split(":")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private Sort createSort(List<SortInput> sortInputs) {
        if (sortInputs == null || sortInputs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> orders = sortInputs.stream()
                .map(input -> new Sort.Order(
                        input.getDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                        input.getField()))
                .collect(Collectors.toList());

        return Sort.by(orders);
    }

    @Data
    @AllArgsConstructor
    public static class SortInput {
        private String field;
        private SortDirection direction;
    }

    public enum SortDirection {
        ASC, DESC
    }
}