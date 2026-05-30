-- Enhance locations table with additional fields for location management
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100) UNIQUE,
    ADD COLUMN IF NOT EXISTS qr_data TEXT,
    ADD COLUMN IF NOT EXISTS capacity INTEGER,
    ADD COLUMN IF NOT EXISTS occupied INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS zone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Create location_stock table for tracking stock per location
CREATE TABLE IF NOT EXISTS location_stock (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity DECIMAL(15, 2) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(15, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(location_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_location_stock_location_id ON location_stock(location_id);
CREATE INDEX IF NOT EXISTS idx_location_stock_product_id ON location_stock(product_id);
