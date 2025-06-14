-- Create product_categories junction table for many-to-many relationship
-- 
-- This migration creates the junction table between products and categories with:
-- - Composite primary key on (product_id, category_id)
-- - Foreign key constraints to both products and categories tables
-- - Additional metadata for the relationship
-- - Optimized indexes for query performance

-- Create product_categories junction table
CREATE TABLE product_categories (
    product_id UUID NOT NULL,
    category_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    
    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    
    -- Composite primary key
    PRIMARY KEY (product_id, category_id),
    
    -- Foreign key constraints
    CONSTRAINT pc_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT pc_category_fk FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_product_categories_product ON product_categories (product_id);
CREATE INDEX idx_product_categories_category ON product_categories (category_id);
CREATE INDEX idx_product_categories_primary ON product_categories (product_id, is_primary) WHERE is_primary = true;
CREATE INDEX idx_product_categories_sort_order ON product_categories (category_id, sort_order);
CREATE INDEX idx_product_categories_created_at ON product_categories (created_at);

-- Create partial index for primary category relationships only
CREATE UNIQUE INDEX idx_product_categories_one_primary 
ON product_categories (product_id) 
WHERE is_primary = true;

-- Function to ensure only one primary category per product
CREATE OR REPLACE FUNCTION ensure_one_primary_category()
RETURNS TRIGGER AS $$
BEGIN
    -- When setting a category as primary, unset others
    IF NEW.is_primary = true THEN
        UPDATE product_categories 
        SET is_primary = false 
        WHERE product_id = NEW.product_id 
            AND category_id != NEW.category_id 
            AND is_primary = true;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to manage primary category assignment
CREATE TRIGGER trigger_product_categories_primary
    BEFORE INSERT OR UPDATE OF is_primary ON product_categories
    FOR EACH ROW
    EXECUTE FUNCTION ensure_one_primary_category();

-- Function to automatically set first category as primary
CREATE OR REPLACE FUNCTION auto_set_primary_category()
RETURNS TRIGGER AS $$
BEGIN
    -- If this is the first category for a product, make it primary
    IF TG_OP = 'INSERT' THEN
        IF NOT EXISTS (
            SELECT 1 FROM product_categories 
            WHERE product_id = NEW.product_id 
            AND category_id != NEW.category_id
        ) THEN
            NEW.is_primary = true;
        END IF;
    END IF;
    
    -- When deleting a primary category, assign primary to another category
    IF TG_OP = 'DELETE' THEN
        IF OLD.is_primary = true THEN
            UPDATE product_categories 
            SET is_primary = true 
            WHERE product_id = OLD.product_id 
                AND category_id != OLD.category_id 
            ORDER BY sort_order, created_at 
            LIMIT 1;
        END IF;
        RETURN OLD;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic primary category management
CREATE TRIGGER trigger_product_categories_auto_primary
    BEFORE INSERT OR DELETE ON product_categories
    FOR EACH ROW
    EXECUTE FUNCTION auto_set_primary_category();

-- Function to validate category assignment (business rules)
CREATE OR REPLACE FUNCTION validate_category_assignment()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if category is active
    IF NOT EXISTS (
        SELECT 1 FROM categories 
        WHERE id = NEW.category_id AND is_active = true
    ) THEN
        RAISE EXCEPTION 'Cannot assign product to inactive category';
    END IF;
    
    -- Check if product is active (optional business rule)
    IF NOT EXISTS (
        SELECT 1 FROM products 
        WHERE id = NEW.product_id AND status != 'DISCONTINUED'
    ) THEN
        RAISE WARNING 'Assigning category to discontinued product: %', NEW.product_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for category assignment validation
CREATE TRIGGER trigger_product_categories_validation
    BEFORE INSERT OR UPDATE ON product_categories
    FOR EACH ROW
    EXECUTE FUNCTION validate_category_assignment();

-- View for easy querying of product categories with category information
CREATE VIEW v_product_categories AS
SELECT 
    pc.product_id,
    pc.category_id,
    pc.is_primary,
    pc.sort_order,
    pc.created_at as assigned_at,
    pc.created_by as assigned_by,
    c.name as category_name,
    c.path as category_path,
    c.level as category_level,
    c.is_active as category_active,
    p.name as product_name,
    p.sku as product_sku,
    p.status as product_status
FROM product_categories pc
JOIN categories c ON pc.category_id = c.id
JOIN products p ON pc.product_id = p.id;

-- View for product category hierarchy (shows all parent categories for products)
CREATE VIEW v_product_category_hierarchy AS
WITH category_hierarchy AS (
    -- Get all categories in the path for each product category
    SELECT 
        pc.product_id,
        c.id as category_id,
        c.name as category_name,
        c.level,
        pc.is_primary,
        regexp_split_to_table(c.path, '/') as path_element
    FROM product_categories pc
    JOIN categories c ON pc.category_id = c.id
)
SELECT DISTINCT
    ch.product_id,
    ch.category_id as direct_category_id,
    ch.category_name as direct_category_name,
    ch.is_primary,
    parent_cat.id as parent_category_id,
    parent_cat.name as parent_category_name,
    parent_cat.level as parent_level
FROM category_hierarchy ch
JOIN categories parent_cat ON parent_cat.id::text = ch.path_element
WHERE parent_cat.id != ch.category_id;

-- Add comments for documentation
COMMENT ON TABLE product_categories IS 'Junction table for many-to-many relationship between products and categories';
COMMENT ON COLUMN product_categories.product_id IS 'Reference to the product';
COMMENT ON COLUMN product_categories.category_id IS 'Reference to the category';
COMMENT ON COLUMN product_categories.is_primary IS 'Whether this is the primary category for the product';
COMMENT ON COLUMN product_categories.sort_order IS 'Display order of product within the category';
COMMENT ON COLUMN product_categories.created_at IS 'Timestamp when the relationship was created';
COMMENT ON COLUMN product_categories.created_by IS 'User ID who created the relationship';

COMMENT ON VIEW v_product_categories IS 'View showing product-category relationships with denormalized category and product information';
COMMENT ON VIEW v_product_category_hierarchy IS 'View showing complete category hierarchy for products including parent categories';