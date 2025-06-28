package com.erpmicroservices.productdomain.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Category entity representing a product category in the system.
 * Supports hierarchical category structure.
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_parent", columnList = "parent_id"),
    @Index(name = "idx_category_path", columnList = "path"),
    @Index(name = "idx_category_active", columnList = "is_active"),
    @Index(name = "idx_category_slug", columnList = "slug", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"parent", "children", "products"})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC, name ASC")
    private Set<Category> children = new HashSet<>();

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "image_url")
    private String imageUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @JsonIgnore
    private Set<Product> products = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @Version
    private Long version;

    /**
     * Build the full path for this category.
     */
    public void buildPath() {
        if (parent == null) {
            this.path = "/";
            this.level = 0;
        } else {
            this.path = parent.getPath() + parent.getId() + "/";
            this.level = parent.getLevel() + 1;
        }
    }

    /**
     * Check if this category is a descendant of the given category.
     */
    public boolean isDescendantOf(Category category) {
        return this.path.contains("/" + category.getId() + "/");
    }

    /**
     * Get the count of child categories.
     */
    @Transient
    public int getChildrenCount() {
        return children.size();
    }

    /**
     * Get the count of products in this category.
     */
    @Transient
    public int getProductCount() {
        return products.size();
    }
}