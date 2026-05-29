-- Create inventory sessions and items tables
CREATE TABLE IF NOT EXISTS inventory_sessions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    status VARCHAR(20) DEFAULT 'OPEN',
    warehouse_id BIGINT REFERENCES locations(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES inventory_sessions(id) ON DELETE CASCADE,
    location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    expected_quantity DECIMAL(15, 2) DEFAULT 0,
    counted_quantity DECIMAL(15, 2),
    scanned_at TIMESTAMP,
    scanned_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_inventory_items_session_id ON inventory_items(session_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_location_id ON inventory_items(location_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_product_id ON inventory_items(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_items_session_location_product
    ON inventory_items(session_id, location_id, product_id);
