-- Create product_variants table for product variations
-- 
-- This migration creates the product_variants table with:
-- - Foreign key relationship to products table
-- - Unique SKU constraint for variant identification
-- - Flexible attributes system using JSONB
-- - Stock quantity tracking
-- - Comprehensive indexing for performance

-- Create product_variants table
CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) CHECK (price >= 0),
    cost DECIMAL(10,2) CHECK (cost >= 0),
    weight DECIMAL(8,3) CHECK (weight >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    reorder_level INTEGER DEFAULT 0 CHECK (reorder_level >= 0),
    max_stock_level INTEGER CHECK (max_stock_level >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Variant-specific attributes (size, color, material, etc.)
    attributes JSONB NOT NULL DEFAULT '{}',
    
    -- Dimensions specific to this variant
    dimensions JSONB,
    
    -- Barcode and other identifiers
    barcode VARCHAR(100),
    upc VARCHAR(20),
    isbn VARCHAR(20),
    
    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT variants_product_fk FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    
    -- Unique constraints
    CONSTRAINT variants_sku_unique UNIQUE (sku),
    CONSTRAINT variants_barcode_unique UNIQUE (barcode),
    CONSTRAINT variants_upc_unique UNIQUE (upc),
    CONSTRAINT variants_isbn_unique UNIQUE (isbn),
    
    -- Business rule constraints
    CONSTRAINT variants_stock_check CHECK (reserved_quantity <= stock_quantity),
    CONSTRAINT variants_reorder_max_check CHECK (reorder_level <= max_stock_level OR max_stock_level IS NULL),
    
    -- Only one default variant per product
    CONSTRAINT variants_one_default_per_product UNIQUE (product_id, is_default) DEFERRABLE INITIALLY DEFERRED
);

-- Create partial index for default variants only (more efficient)
CREATE UNIQUE INDEX idx_variants_product_default 
ON product_variants (product_id) 
WHERE is_default = true;

-- Create indexes for performance
CREATE INDEX idx_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_variants_sku ON product_variants (sku);
CREATE INDEX idx_variants_barcode ON product_variants (barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_variants_upc ON product_variants (upc) WHERE upc IS NOT NULL;
CREATE INDEX idx_variants_isbn ON product_variants (isbn) WHERE isbn IS NOT NULL;
CREATE INDEX idx_variants_is_active ON product_variants (is_active);
CREATE INDEX idx_variants_stock_quantity ON product_variants (stock_quantity);
CREATE INDEX idx_variants_reorder_level ON product_variants (reorder_level) WHERE reorder_level > 0;
CREATE INDEX idx_variants_created_at ON product_variants (created_at);
CREATE INDEX idx_variants_attributes ON product_variants USING gin (attributes);
CREATE INDEX idx_variants_name ON product_variants USING gin (to_tsvector('english', name));

-- Create composite indexes for common query patterns
CREATE INDEX idx_variants_product_active ON product_variants (product_id, is_active);
CREATE INDEX idx_variants_product_stock ON product_variants (product_id, stock_quantity);
CREATE INDEX idx_variants_low_stock ON product_variants (product_id, stock_quantity, reorder_level) 
WHERE is_active = true AND stock_quantity <= reorder_level;

-- Create trigger to automatically update updated_at on product_variants table
CREATE TRIGGER trigger_variants_updated_at
    BEFORE UPDATE ON product_variants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to ensure at least one default variant per product
CREATE OR REPLACE FUNCTION ensure_default_variant()
RETURNS TRIGGER AS $$
BEGIN
    -- When inserting the first variant for a product, make it default
    IF TG_OP = 'INSERT' THEN
        IF NOT EXISTS (SELECT 1 FROM product_variants WHERE product_id = NEW.product_id) THEN
            NEW.is_default = true;
        END IF;
    END IF;
    
    -- When deleting a default variant, assign default to another active variant
    IF TG_OP = 'DELETE' THEN
        IF OLD.is_default = true THEN
            UPDATE product_variants 
            SET is_default = true 
            WHERE product_id = OLD.product_id 
                AND is_active = true 
                AND id != OLD.id 
            ORDER BY created_at 
            LIMIT 1;
        END IF;
        RETURN OLD;
    END IF;
    
    -- When updating default status, ensure only one default per product
    IF TG_OP = 'UPDATE' AND NEW.is_default = true AND OLD.is_default = false THEN
        UPDATE product_variants 
        SET is_default = false 
        WHERE product_id = NEW.product_id 
            AND id != NEW.id 
            AND is_default = true;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to manage default variants
CREATE TRIGGER trigger_variants_default_management
    BEFORE INSERT OR UPDATE OR DELETE ON product_variants
    FOR EACH ROW
    EXECUTE FUNCTION ensure_default_variant();

-- Function to update product price when default variant price changes
CREATE OR REPLACE FUNCTION sync_product_price_from_variant()
RETURNS TRIGGER AS $$
BEGIN
    -- Update product price when default variant price changes
    IF NEW.is_default = true AND (OLD IS NULL OR NEW.price != OLD.price) THEN
        UPDATE products 
        SET price = NEW.price, 
            updated_at = CURRENT_TIMESTAMP,
            updated_by = NEW.updated_by
        WHERE id = NEW.product_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to sync product price from default variant
CREATE TRIGGER trigger_variants_sync_product_price
    AFTER INSERT OR UPDATE OF price, is_default ON product_variants
    FOR EACH ROW
    EXECUTE FUNCTION sync_product_price_from_variant();

-- Add comments for documentation
COMMENT ON TABLE product_variants IS 'Product variants table for storing product variations (size, color, etc.)';
COMMENT ON COLUMN product_variants.id IS 'Unique identifier for the product variant (UUID)';
COMMENT ON COLUMN product_variants.product_id IS 'Reference to the parent product';
COMMENT ON COLUMN product_variants.name IS 'Display name of the variant (e.g., "Large Red T-Shirt")';
COMMENT ON COLUMN product_variants.sku IS 'Stock Keeping Unit - unique variant identifier';
COMMENT ON COLUMN product_variants.price IS 'Selling price of this variant (overrides product price if set)';
COMMENT ON COLUMN product_variants.cost IS 'Cost price of this variant';
COMMENT ON COLUMN product_variants.weight IS 'Weight of this variant in kilograms';
COMMENT ON COLUMN product_variants.stock_quantity IS 'Current stock quantity available';
COMMENT ON COLUMN product_variants.reserved_quantity IS 'Quantity reserved for pending orders';
COMMENT ON COLUMN product_variants.reorder_level IS 'Stock level that triggers reorder notification';
COMMENT ON COLUMN product_variants.max_stock_level IS 'Maximum stock level for inventory management';
COMMENT ON COLUMN product_variants.is_active IS 'Whether this variant is active and available for sale';
COMMENT ON COLUMN product_variants.is_default IS 'Whether this is the default variant for the product';
COMMENT ON COLUMN product_variants.attributes IS 'Variant attributes as JSON (color, size, material, etc.)';
COMMENT ON COLUMN product_variants.dimensions IS 'Variant-specific dimensions as JSON';
COMMENT ON COLUMN product_variants.barcode IS 'Barcode for this variant';
COMMENT ON COLUMN product_variants.upc IS 'Universal Product Code';
COMMENT ON COLUMN product_variants.isbn IS 'International Standard Book Number (for books)';
COMMENT ON COLUMN product_variants.created_at IS 'Timestamp when the variant was created';
COMMENT ON COLUMN product_variants.updated_at IS 'Timestamp when the variant was last updated';
COMMENT ON COLUMN product_variants.created_by IS 'User ID who created the variant';
COMMENT ON COLUMN product_variants.updated_by IS 'User ID who last updated the variant';