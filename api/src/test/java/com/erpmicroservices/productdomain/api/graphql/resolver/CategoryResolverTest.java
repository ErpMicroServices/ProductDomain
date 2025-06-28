package com.erpmicroservices.productdomain.api.graphql.resolver;

import com.erpmicroservices.productdomain.api.graphql.model.input.CreateCategoryInput;
import com.erpmicroservices.productdomain.api.graphql.model.input.UpdateCategoryInput;
import com.erpmicroservices.productdomain.api.graphql.model.filter.CategoryFilter;
import com.erpmicroservices.productdomain.api.service.CategoryService;
import com.erpmicroservices.productdomain.database.entity.Category;
import graphql.GraphQLException;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Resolver Unit Tests")
class CategoryResolverTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryResolver categoryResolver;

    private Category testCategory;
    private Category parentCategory;
    private Category childCategory;
    private UUID categoryId;
    private UUID parentId;
    private UUID childId;
    private CreateCategoryInput createInput;
    private UpdateCategoryInput updateInput;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        childId = UUID.randomUUID();

        parentCategory = Category.builder()
                .id(parentId)
                .name("Parent Category")
                .description("Parent Description")
                .level(0)
                .path("/")
                .createdAt(LocalDateTime.now())
                .build();

        testCategory = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .description("Test Description")
                .parent(parentCategory)
                .level(1)
                .path("/" + parentId + "/")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        childCategory = Category.builder()
                .id(childId)
                .name("Child Category")
                .description("Child Description")
                .parent(testCategory)
                .level(2)
                .path("/" + parentId + "/" + categoryId + "/")
                .createdAt(LocalDateTime.now())
                .build();

        createInput = CreateCategoryInput.builder()
                .name("New Category")
                .slug("new-category")
                .description("New Description")
                .parentId(parentId)
                .build();

        updateInput = UpdateCategoryInput.builder()
                .name("Updated Category")
                .description("Updated Description")
                .build();
    }

    @Test
    @DisplayName("Should retrieve category by ID successfully")
    void shouldRetrieveCategoryById() {
        // Given
        when(categoryService.findById(categoryId)).thenReturn(Optional.of(testCategory));

        // When
        Category result = categoryResolver.category(categoryId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getName()).isEqualTo("Test Category");
        assertThat(result.getParent()).isEqualTo(parentCategory);
        verify(categoryService).findById(categoryId);
    }

    @Test
    @DisplayName("Should throw exception when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Given
        when(categoryService.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryResolver.category(categoryId))
                .isInstanceOf(GraphQLException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Should retrieve root categories")
    void shouldRetrieveRootCategories() {
        // Given
        CategoryFilter filter = CategoryFilter.builder()
                .isRoot(true)
                .build();
        List<Category> rootCategories = Arrays.asList(parentCategory);
        Page<Category> categoryPage = new PageImpl<>(rootCategories, PageRequest.of(0, 10), 1);
        
        when(categoryService.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(categoryPage);

        // When
        var connection = categoryResolver.categories(10, null, filter);

        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.getEdges()).hasSize(1);
        assertThat(connection.getEdges().get(0).getNode().getParent()).isNull();
        verify(categoryService).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should create category with parent successfully")
    void shouldCreateCategoryWithParentSuccessfully() {
        // Given
        Category newCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("New Category")
                .description("New Description")
                .parent(parentCategory)
                .level(1)
                .path("/" + parentId + "/")
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryService.findById(parentId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.create(any(Category.class))).thenReturn(newCategory);

        // When
        Category result = categoryResolver.createCategory(createInput);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Category");
        assertThat(result.getParent()).isEqualTo(parentCategory);
        assertThat(result.getLevel()).isEqualTo(1);
        verify(categoryService).create(any(Category.class));
    }

    @Test
    @DisplayName("Should create root category successfully")
    void shouldCreateRootCategorySuccessfully() {
        // Given
        CreateCategoryInput rootInput = CreateCategoryInput.builder()
                .name("Root Category")
                .slug("root-category")
                .description("Root Description")
                .build();

        Category rootCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Root Category")
                .description("Root Description")
                .level(0)
                .path("/")
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryService.create(any(Category.class))).thenReturn(rootCategory);

        // When
        Category result = categoryResolver.createCategory(rootInput);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParent()).isNull();
        assertThat(result.getLevel()).isEqualTo(0);
        assertThat(result.getPath()).isEqualTo("/");
        verify(categoryService).create(any(Category.class));
    }

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() {
        // Given
        Category updatedCategory = Category.builder()
                .id(categoryId)
                .name("Updated Category")
                .description("Updated Description")
                .parent(parentCategory)
                .level(1)
                .path("/" + parentId + "/")
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryService.update(eq(categoryId), any(Category.class)))
                .thenReturn(updatedCategory);

        // When
        Category result = categoryResolver.updateCategory(categoryId, updateInput);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Category");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(categoryService).update(eq(categoryId), any(Category.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void shouldDeleteCategorySuccessfully() {
        // Given
        when(categoryService.existsById(categoryId)).thenReturn(true);

        // When
        Boolean result = categoryResolver.deleteCategory(categoryId);

        // Then
        assertThat(result).isTrue();
        verify(categoryService).deleteById(categoryId);
    }


    @Test
    @DisplayName("Should retrieve category children")
    void shouldRetrieveCategoryChildren() {
        // Given
        testCategory.setChildren(Set.of(childCategory));
        when(categoryService.findById(categoryId)).thenReturn(Optional.of(testCategory));

        // When
        Category result = categoryResolver.category(categoryId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren()).extracting(Category::getName).containsExactly("Child Category");
    }



    @Test
    @DisplayName("Should validate category depth limit")
    void shouldValidateCategoryDepthLimit() {
        // Given
        CreateCategoryInput deepInput = CreateCategoryInput.builder()
                .name("Too Deep Category")
                .slug("too-deep-category")
                .parentId(childId)
                .build();

        when(categoryService.findById(childId)).thenReturn(Optional.of(childCategory));
        when(categoryService.create(any(Category.class)))
                .thenThrow(new IllegalArgumentException("Maximum category depth exceeded"));

        // When/Then
        assertThatThrownBy(() -> categoryResolver.createCategory(deepInput))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum category depth exceeded");
    }

}