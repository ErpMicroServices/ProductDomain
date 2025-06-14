-- Create products table with comprehensive structure for product management
-- 
-- This migration creates the core products table with:
-- - UUID primary key for distributed system compatibility
-- - SKU with unique constraint for product identification
-- - Comprehensive product information fields
-- - Audit columns for tracking changes
-- - Performance indexes

-- Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    cost DECIMAL(10,2) CHECK (cost >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    weight DECIMAL(8,3) CHECK (weight >= 0),
    dimensions JSONB,
    attributes JSONB,
    tags TEXT[],
    
    -- Audit columns
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT products_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED', 'DRAFT')),
    CONSTRAINT products_sku_unique UNIQUE (sku)
);

-- Create indexes for performance
CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products USING gin (to_tsvector('english', name));
CREATE INDEX idx_products_created_at ON products (created_at);
CREATE INDEX idx_products_updated_at ON products (updated_at);
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_tags ON products USING gin (tags);
CREATE INDEX idx_products_attributes ON products USING gin (attributes);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on products table
CREATE TRIGGER trigger_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE products IS 'Core products table storing product information for the product domain';
COMMENT ON COLUMN products.id IS 'Unique identifier for the product (UUID)';
COMMENT ON COLUMN products.name IS 'Display name of the product';
COMMENT ON COLUMN products.description IS 'Detailed description of the product';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN products.price IS 'Selling price of the product';
COMMENT ON COLUMN products.cost IS 'Cost price of the product';
COMMENT ON COLUMN products.status IS 'Current status of the product (ACTIVE, INACTIVE, DISCONTINUED, DRAFT)';
COMMENT ON COLUMN products.weight IS 'Weight of the product in kilograms';
COMMENT ON COLUMN products.dimensions IS 'Product dimensions stored as JSON (length, width, height)';
COMMENT ON COLUMN products.attributes IS 'Additional product attributes stored as JSON';
COMMENT ON COLUMN products.tags IS 'Array of tags for product categorization and search';
COMMENT ON COLUMN products.created_at IS 'Timestamp when the product was created';
COMMENT ON COLUMN products.updated_at IS 'Timestamp when the product was last updated';
COMMENT ON COLUMN products.created_by IS 'User ID who created the product';
COMMENT ON COLUMN products.updated_by IS 'User ID who last updated the product';