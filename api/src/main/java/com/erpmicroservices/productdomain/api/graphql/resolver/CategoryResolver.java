package com.erpmicroservices.productdomain.api.graphql.resolver;

import com.erpmicroservices.productdomain.api.graphql.model.connection.CategoryConnection;
import com.erpmicroservices.productdomain.api.graphql.model.connection.CategoryEdge;
import com.erpmicroservices.productdomain.api.graphql.model.connection.PageInfo;
import com.erpmicroservices.productdomain.api.graphql.model.filter.CategoryFilter;
import com.erpmicroservices.productdomain.api.graphql.model.input.CreateCategoryInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateCategoryInput;
import com.erpmicroservices.productdomain.api.service.CategoryService;
import com.erpmicroservices.productdomain.database.entity.Category;
import graphql.GraphQLException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CategoryResolver {
    
    private final CategoryService categoryService;
    
    @QueryMapping
    public Category category(@Argument @NotNull UUID id) {
        log.debug("Fetching category with ID: {}", id);
        return categoryService.findById(id)
                .orElseThrow(() -> new GraphQLException("Category not found with ID: " + id));
    }
    
    @QueryMapping
    public CategoryConnection categories(
            @Argument Integer first,
            @Argument String after,
            @Argument CategoryFilter filter) {
        log.debug("Fetching categories - first: {}, after: {}, filter: {}", first, after, filter);
        
        int pageSize = first != null ? first : 20;
        int page = 0;
        
        if (after != null) {
            try {
                page = Integer.parseInt(new String(java.util.Base64.getDecoder().decode(after)));
            } catch (Exception e) {
                log.warn("Invalid cursor: {}", after);
            }
        }
        
        Pageable pageable = PageRequest.of(page, pageSize);
        Specification<Category> spec = buildSpecification(filter);
        Page<Category> categoryPage = categoryService.findAll(spec, pageable);
        
        return buildConnection(categoryPage);
    }
    
    @QueryMapping
    public List<Category> rootCategories() {
        log.debug("Fetching root categories");
        return categoryService.findRootCategories();
    }
    
    @QueryMapping
    public List<Category> categoryPath(@Argument @NotNull UUID categoryId) {
        log.debug("Fetching category path for ID: {}", categoryId);
        return categoryService.getCategoryPath(categoryId);
    }
    
    @MutationMapping
    public Category createCategory(@Argument @Valid CreateCategoryInput input) {
        log.info("Creating category: {}", input);
        
        Category category = new Category();
        category.setName(input.getName());
        category.setSlug(input.getSlug());
        category.setDescription(input.getDescription());
        
        if (input.getParentId() != null) {
            Category parent = categoryService.findById(input.getParentId())
                    .orElseThrow(() -> new GraphQLException(
                        "Parent category not found with ID: " + input.getParentId()));
            category.setParent(parent);
        }
        
        if (input.getAttributes() != null) {
            category.setAttributes(input.getAttributes());
        }
        
        return categoryService.create(category);
    }
    
    @MutationMapping
    public Category updateCategory(@Argument @NotNull UUID id, @Argument @Valid UpdateCategoryInput input) {
        log.info("Updating category ID: {} with input: {}", id, input);
        
        Category existing = categoryService.findById(id)
                .orElseThrow(() -> new GraphQLException("Category not found with ID: " + id));
        
        if (input.getName() != null) {
            existing.setName(input.getName());
        }
        if (input.getSlug() != null) {
            existing.setSlug(input.getSlug());
        }
        if (input.getDescription() != null) {
            existing.setDescription(input.getDescription());
        }
        if (input.getAttributes() != null) {
            existing.setAttributes(input.getAttributes());
        }
        if (input.getParentId() != null) {
            Category parent = categoryService.findById(input.getParentId())
                    .orElseThrow(() -> new GraphQLException(
                        "Parent category not found with ID: " + input.getParentId()));
            existing.setParent(parent);
        }
        
        return categoryService.update(id, existing);
    }
    
    @MutationMapping
    public Boolean deleteCategory(@Argument @NotNull UUID id) {
        log.info("Deleting category with ID: {}", id);
        
        if (!categoryService.existsById(id)) {
            throw new GraphQLException("Category not found with ID: " + id);
        }
        
        categoryService.deleteById(id);
        return true;
    }
    
    @SubscriptionMapping
    public Flux<Category> categoryUpdated(@Argument UUID categoryId) {
        log.debug("Subscribing to category updates for ID: {}", categoryId);
        return categoryService.subscribeToCategoryUpdates(categoryId);
    }
    
    private Specification<Category> buildSpecification(CategoryFilter filter) {
        if (filter == null) {
            return Specification.where(null);
        }
        
        Specification<Category> spec = Specification.where(null);
        
        if (filter.getName() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
        }
        
        if (filter.getSlug() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("slug"), filter.getSlug()));
        }
        
        if (filter.getParentId() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("parent").get("id"), filter.getParentId()));
        }
        
        if (filter.getIsRoot() != null && filter.getIsRoot()) {
            spec = spec.and((root, query, cb) -> 
                cb.isNull(root.get("parent")));
        }
        
        return spec;
    }
    
    private CategoryConnection buildConnection(Page<Category> page) {
        List<CategoryEdge> edges = page.getContent().stream()
                .map(category -> {
                    String cursor = java.util.Base64.getEncoder()
                            .encodeToString(String.valueOf(page.getNumber()).getBytes());
                    return CategoryEdge.builder()
                            .node(category)
                            .cursor(cursor)
                            .build();
                })
                .collect(Collectors.toList());
        
        PageInfo pageInfo = PageInfo.builder()
                .hasNextPage(page.hasNext())
                .hasPreviousPage(page.hasPrevious())
                .startCursor(edges.isEmpty() ? null : edges.get(0).getCursor())
                .endCursor(edges.isEmpty() ? null : edges.get(edges.size() - 1).getCursor())
                .build();
        
        return CategoryConnection.builder()
                .edges(edges)
                .pageInfo(pageInfo)
                .totalCount((int) page.getTotalElements())
                .build();
    }
}