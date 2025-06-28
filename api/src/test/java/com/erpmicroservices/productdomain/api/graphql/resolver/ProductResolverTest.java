package com.erpmicroservices.productdomain.api.graphql.resolver;

import com.erpmicroservices.productdomain.api.graphql.model.input.CreateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateProductInput;
import com.erpmicroservices.productdomain.api.graphql.model.filter.ProductFilter;
import com.erpmicroservices.productdomain.api.service.ProductService;
import com.erpmicroservices.productdomain.model.Category;
import com.erpmicroservices.productdomain.model.Product;
import com.erpmicroservices.productdomain.model.ProductVariant;
import graphql.GraphQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Resolver Unit Tests")
class ProductResolverTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductResolver productResolver;

    private Product testProduct;
    private UUID productId;
    private CreateProductInput createInput;
    private UpdateProductInput updateInput;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        
        testProduct = Product.builder()
                .id(productId)
                .sku("TEST-001")
                .name("Test Product")
                .description("Test Description")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createInput = CreateProductInput.builder()
                .sku("NEW-001")
                .name("New Product")
                .description("New Description")
                .active(true)
                .build();

        updateInput = UpdateProductInput.builder()
                .name("Updated Product")
                .description("Updated Description")
                .build();
    }

    @Test
    @DisplayName("Should retrieve product by ID successfully")
    void shouldRetrieveProductById() {
        // Given
        when(productService.findById(productId)).thenReturn(Optional.of(testProduct));

        // When
        Product result = productResolver.product(productId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getSku()).isEqualTo("TEST-001");
        verify(productService).findById(productId);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productService.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> productResolver.product(productId))
                .isInstanceOf(GraphQLException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should retrieve products with pagination")
    void shouldRetrieveProductsWithPagination() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);
        when(productService.findAll(any(ProductFilter.class), any(PageRequest.class)))
                .thenReturn(productPage);

        // When
        var connection = productResolver.products(10, null, null, null);

        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.getEdges()).hasSize(1);
        assertThat(connection.getTotalCount()).isEqualTo(1);
        assertThat(connection.getPageInfo().isHasNextPage()).isFalse();
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProductSuccessfully() {
        // Given
        when(productService.create(any(CreateProductInput.class))).thenReturn(testProduct);

        // When
        Product result = productResolver.createProduct(createInput);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001");
        verify(productService).create(createInput);
    }

    @Test
    @DisplayName("Should validate required fields on product creation")
    void shouldValidateRequiredFieldsOnCreation() {
        // Given
        CreateProductInput invalidInput = CreateProductInput.builder().build();

        // When/Then
        assertThatThrownBy(() -> productResolver.createProduct(invalidInput))
                .isInstanceOf(GraphQLException.class)
                .hasMessageContaining("validation");
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProductSuccessfully() {
        // Given
        Product updatedProduct = Product.builder()
                .id(productId)
                .sku("TEST-001")
                .name("Updated Product")
                .description("Updated Description")
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(productService.update(eq(productId), any(UpdateProductInput.class)))
                .thenReturn(updatedProduct);

        // When
        Product result = productResolver.updateProduct(productId, updateInput);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(productService).update(productId, updateInput);
    }

    @Test
    @DisplayName("Should delete product successfully")
    void shouldDeleteProductSuccessfully() {
        // Given
        when(productService.delete(productId)).thenReturn(true);

        // When
        boolean result = productResolver.deleteProduct(productId);

        // Then
        assertThat(result).isTrue();
        verify(productService).delete(productId);
    }

    @Test
    @DisplayName("Should return false when delete fails")
    void shouldReturnFalseWhenDeleteFails() {
        // Given
        when(productService.delete(productId)).thenReturn(false);

        // When
        boolean result = productResolver.deleteProduct(productId);

        // Then
        assertThat(result).isFalse();
        verify(productService).delete(productId);
    }

    @Test
    @DisplayName("Should add product to category successfully")
    void shouldAddProductToCategorySuccessfully() {
        // Given
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .build();
        
        Product productWithCategory = Product.builder()
                .id(productId)
                .sku("TEST-001")
                .name("Test Product")
                .categories(Set.of(category))
                .build();

        when(productService.addToCategory(productId, categoryId))
                .thenReturn(productWithCategory);

        // When
        Product result = productResolver.addProductToCategory(productId, categoryId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories()).extracting(Category::getId).containsExactly(categoryId);
        verify(productService).addToCategory(productId, categoryId);
    }

    @Test
    @DisplayName("Should remove product from category successfully")
    void shouldRemoveProductFromCategorySuccessfully() {
        // Given
        UUID categoryId = UUID.randomUUID();
        when(productService.removeFromCategory(productId, categoryId))
                .thenReturn(testProduct);

        // When
        Product result = productResolver.removeProductFromCategory(productId, categoryId);

        // Then
        assertThat(result).isNotNull();
        verify(productService).removeFromCategory(productId, categoryId);
    }

    @Test
    @DisplayName("Should handle product with variants")
    void shouldHandleProductWithVariants() {
        // Given
        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .sku("TEST-001-VAR")
                .name("Test Variant")
                .price(new BigDecimal("29.99"))
                .build();
        
        Product productWithVariant = Product.builder()
                .id(productId)
                .sku("TEST-001")
                .name("Test Product")
                .variants(List.of(variant))
                .build();

        when(productService.findById(productId)).thenReturn(Optional.of(productWithVariant));

        // When
        Product result = productResolver.product(productId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVariants()).hasSize(1);
        assertThat(result.getVariants().get(0).getSku()).isEqualTo("TEST-001-VAR");
    }

    @Test
    @DisplayName("Should filter products by category")
    void shouldFilterProductsByCategory() {
        // Given
        UUID categoryId = UUID.randomUUID();
        ProductFilter filter = ProductFilter.builder()
                .categoryIds(List.of(categoryId))
                .build();

        List<Product> filteredProducts = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(filteredProducts, PageRequest.of(0, 10), 1);
        
        when(productService.findAll(eq(filter), any(PageRequest.class)))
                .thenReturn(productPage);

        // When
        var connection = productResolver.products(10, null, filter, null);

        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.getEdges()).hasSize(1);
        verify(productService).findAll(eq(filter), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should handle concurrent updates gracefully")
    void shouldHandleConcurrentUpdatesGracefully() {
        // Given
        when(productService.update(eq(productId), any(UpdateProductInput.class)))
                .thenThrow(new OptimisticLockingFailureException("Version mismatch"));

        // When/Then
        assertThatThrownBy(() -> productResolver.updateProduct(productId, updateInput))
                .isInstanceOf(GraphQLException.class)
                .hasMessageContaining("concurrent update");
    }

    @Test
    @DisplayName("Should validate SKU uniqueness on creation")
    void shouldValidateSKUUniquenessOnCreation() {
        // Given
        when(productService.create(any(CreateProductInput.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate SKU"));

        // When/Then
        assertThatThrownBy(() -> productResolver.createProduct(createInput))
                .isInstanceOf(GraphQLException.class)
                .hasMessageContaining("SKU already exists");
    }
}