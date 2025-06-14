-- Create categories table with hierarchical structure
-- 
-- This migration creates the categories table with:
-- - Self-referencing foreign key for hierarchy
-- - Path column for efficient tree operations
-- - Level column for depth tracking
-- - Comprehensive indexing for performance

-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID,
    path VARCHAR(1000) NOT NULL,
    level INTEGER NOT NULL DEFAULT 0 CHECK (level >= 0),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500),
    attributes JSONB,
    
    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Self-referencing foreign key constraint
    CONSTRAINT categories_parent_fk FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE,
    
    -- Ensure root categories have null parent_id
    CONSTRAINT categories_root_check CHECK (
        (parent_id IS NULL AND level = 0) OR 
        (parent_id IS NOT NULL AND level > 0)
    ),
    
    -- Unique constraint on name within the same parent
    CONSTRAINT categories_name_parent_unique UNIQUE (name, parent_id)
);

-- Create indexes for performance
CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_categories_path ON categories USING btree (path);
CREATE INDEX idx_categories_level ON categories (level);
CREATE INDEX idx_categories_name ON categories USING gin (to_tsvector('english', name));
CREATE INDEX idx_categories_is_active ON categories (is_active);
CREATE INDEX idx_categories_sort_order ON categories (sort_order);
CREATE INDEX idx_categories_created_at ON categories (created_at);

-- Create trigger to automatically update updated_at on categories table
CREATE TRIGGER trigger_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to automatically generate and update category path
CREATE OR REPLACE FUNCTION update_category_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path VARCHAR(1000);
BEGIN
    -- If this is a root category (no parent)
    IF NEW.parent_id IS NULL THEN
        NEW.path = NEW.id::text;
        NEW.level = 0;
    ELSE
        -- Get parent's path and level
        SELECT path, level INTO parent_path, NEW.level 
        FROM categories 
        WHERE id = NEW.parent_id;
        
        -- Increment level
        NEW.level = NEW.level + 1;
        
        -- Build new path
        NEW.path = parent_path || '/' || NEW.id::text;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update path and level
CREATE TRIGGER trigger_categories_path
    BEFORE INSERT OR UPDATE OF parent_id ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_category_path();

-- Function to validate category hierarchy depth (prevent too deep nesting)
CREATE OR REPLACE FUNCTION validate_category_depth()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.level > 10 THEN
        RAISE EXCEPTION 'Category hierarchy too deep. Maximum depth is 10 levels.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to validate hierarchy depth
CREATE TRIGGER trigger_categories_depth_validation
    BEFORE INSERT OR UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION validate_category_depth();

-- Function to prevent circular references in category hierarchy
CREATE OR REPLACE FUNCTION prevent_category_circular_reference()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if new parent is a descendant of the current category
    IF NEW.parent_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM categories 
        WHERE path LIKE (NEW.path || '%') 
        AND id = NEW.parent_id
    ) THEN
        RAISE EXCEPTION 'Circular reference detected in category hierarchy';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to prevent circular references
CREATE TRIGGER trigger_categories_circular_reference
    BEFORE UPDATE OF parent_id ON categories
    FOR EACH ROW
    EXECUTE FUNCTION prevent_category_circular_reference();

-- Add comments for documentation
COMMENT ON TABLE categories IS 'Hierarchical categories table for organizing products';
COMMENT ON COLUMN categories.id IS 'Unique identifier for the category (UUID)';
COMMENT ON COLUMN categories.name IS 'Display name of the category';
COMMENT ON COLUMN categories.description IS 'Detailed description of the category';
COMMENT ON COLUMN categories.parent_id IS 'Reference to parent category for hierarchy';
COMMENT ON COLUMN categories.path IS 'Materialized path for efficient tree operations (e.g., "parent_id/child_id/grandchild_id")';
COMMENT ON COLUMN categories.level IS 'Depth level in the hierarchy (0 for root categories)';
COMMENT ON COLUMN categories.sort_order IS 'Display order within the same level';
COMMENT ON COLUMN categories.is_active IS 'Whether the category is active and visible';
COMMENT ON COLUMN categories.image_url IS 'URL to category image or icon';
COMMENT ON COLUMN categories.attributes IS 'Additional category attributes stored as JSON';
COMMENT ON COLUMN categories.created_at IS 'Timestamp when the category was created';
COMMENT ON COLUMN categories.updated_at IS 'Timestamp when the category was last updated';
COMMENT ON COLUMN categories.created_by IS 'User ID who created the category';
COMMENT ON COLUMN categories.updated_by IS 'User ID who last updated the category';