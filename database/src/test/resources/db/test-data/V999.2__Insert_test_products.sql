-- Test data for products table
-- This migration inserts test data for development and testing purposes

-- Insert test products
INSERT INTO products (id, name, description, sku, price, cost, status, weight, dimensions, attributes, tags, created_by, updated_by) VALUES
('660e8400-e29b-41d4-a716-446655440000', 'MacBook Pro 16"', 'Apple MacBook Pro 16-inch with M2 chip', 'MBP-16-M2-001', 2499.00, 1800.00, 'ACTIVE', 2.15, '{"length": 35.57, "width": 24.59, "height": 1.68}', '{"brand": "Apple", "processor": "M2", "memory": "16GB", "storage": "512GB"}', '{"laptop", "apple", "professional", "m2"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440001', 'iPhone 15 Pro', 'Apple iPhone 15 Pro with titanium design', 'IPH-15-PRO-001', 999.00, 650.00, 'ACTIVE', 0.187, '{"length": 14.67, "width": 7.09, "height": 0.83}', '{"brand": "Apple", "storage": "128GB", "color": "Natural Titanium", "screen_size": "6.1"}', '{"smartphone", "apple", "titanium", "pro"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440002', 'Dell XPS 13', 'Dell XPS 13 ultrabook with Intel i7', 'DELL-XPS-13-001', 1299.00, 950.00, 'ACTIVE', 1.27, '{"length": 29.57, "width": 19.86, "height": 1.48}', '{"brand": "Dell", "processor": "Intel i7", "memory": "16GB", "storage": "512GB"}', '{"laptop", "dell", "ultrabook", "business"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440003', 'Classic White T-Shirt', 'Premium cotton white t-shirt', 'TSH-WHT-001', 29.99, 12.50, 'ACTIVE', 0.15, '{"length": 71, "width": 51, "height": 0.5}', '{"material": "100% Cotton", "care": "Machine wash cold", "fit": "Regular"}', '{"clothing", "t-shirt", "cotton", "basic"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440004', 'Wireless Bluetooth Headphones', 'Premium wireless headphones with noise cancellation', 'WH-BT-001', 199.99, 85.00, 'ACTIVE', 0.25, '{"length": 19, "width": 17, "height": 8}', '{"connectivity": "Bluetooth 5.0", "battery_life": "30 hours", "noise_cancellation": true}', '{"audio", "wireless", "bluetooth", "headphones"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440005', 'The Clean Code', 'A Handbook of Agile Software Craftsmanship by Robert C. Martin', 'BOOK-CC-001', 42.99, 18.50, 'ACTIVE', 0.68, '{"length": 23.5, "width": 15.7, "height": 2.8}', '{"author": "Robert C. Martin", "pages": 464, "publisher": "Prentice Hall", "isbn": "978-0132350884"}', '{"book", "programming", "software", "clean-code"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440006', 'Samsung Galaxy S24', 'Samsung Galaxy S24 with AI features', 'SGS-S24-001', 799.99, 520.00, 'ACTIVE', 0.167, '{"length": 14.7, "width": 7.06, "height": 0.76}', '{"brand": "Samsung", "storage": "256GB", "color": "Phantom Black", "screen_size": "6.2"}', '{"smartphone", "samsung", "android", "ai"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440007', 'Gaming Desktop PC', 'High-performance gaming desktop with RTX 4080', 'PC-GAM-001', 2299.99, 1650.00, 'ACTIVE', 12.5, '{"length": 51, "width": 23, "height": 46}', '{"processor": "AMD Ryzen 7", "memory": "32GB", "graphics": "RTX 4080", "storage": "1TB NVMe"}', '{"desktop", "gaming", "pc", "high-performance"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440008', 'Denim Jeans', 'Classic blue denim jeans', 'JNS-DEN-001', 79.99, 35.00, 'ACTIVE', 0.6, '{"length": 110, "width": 40, "height": 2}', '{"material": "98% Cotton, 2% Elastane", "wash": "Medium Blue", "fit": "Slim"}', '{"clothing", "jeans", "denim", "casual"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440009', 'Design Patterns Book', 'Elements of Reusable Object-Oriented Software', 'BOOK-DP-001', 54.99, 22.00, 'ACTIVE', 0.75, '{"length": 23.5, "width": 15.7, "height": 3.2}', '{"authors": "Gang of Four", "pages": 395, "publisher": "Addison-Wesley", "isbn": "978-0201633610"}', '{"book", "programming", "design-patterns", "software"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),

('660e8400-e29b-41d4-a716-446655440010', 'Inactive Product', 'This product is inactive for testing', 'INACT-001', 99.99, 50.00, 'INACTIVE', 1.0, '{"length": 10, "width": 10, "height": 10}', '{"test": true}', '{"test", "inactive"}', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001');