-- Create warehouses table and add warehouse_id to warehouse-scoped tables
-- Backfill with a default warehouse for existing data

DO $$
DECLARE
    default_tenant_id BIGINT;
    default_warehouse_id BIGINT;
BEGIN
    -- Get the default tenant
    SELECT id INTO default_tenant_id FROM tenants ORDER BY id LIMIT 1;
    IF default_tenant_id IS NULL THEN
        default_tenant_id := 1;
    END IF;

    -- Create warehouses table
    CREATE TABLE IF NOT EXISTS warehouses (
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        code VARCHAR(255) NOT NULL,
        is_active BOOLEAN DEFAULT TRUE,
        tenant_id BIGINT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Insert a default warehouse for each existing tenant
    -- (only if no warehouses exist yet)
    IF NOT EXISTS (SELECT 1 FROM warehouses WHERE tenant_id = default_tenant_id) THEN
        INSERT INTO warehouses (name, code, is_active, tenant_id)
        VALUES ('Main Warehouse', 'MAIN', TRUE, default_tenant_id);
    END IF;

    -- Get the default warehouse ID
    SELECT id INTO default_warehouse_id FROM warehouses WHERE tenant_id = default_tenant_id ORDER BY id LIMIT 1;

    -- Add warehouse_id columns (nullable — cross-warehouse queries are possible)
    -- Products
    EXECUTE 'ALTER TABLE products ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE products SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- Locations
    EXECUTE 'ALTER TABLE locations ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE locations SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- LocationStock
    EXECUTE 'ALTER TABLE location_stock ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE location_stock SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- Batches
    EXECUTE 'ALTER TABLE batches ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE batches SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- StockMovements
    EXECUTE 'ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE stock_movements SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- StockReservations
    EXECUTE 'ALTER TABLE stock_reservations ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE stock_reservations SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- WarehouseDocuments
    EXECUTE 'ALTER TABLE warehouse_documents ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE warehouse_documents SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- WarehouseDocumentItems
    EXECUTE 'ALTER TABLE warehouse_document_items ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE warehouse_document_items SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- InventorySessions
    EXECUTE 'ALTER TABLE inventory_sessions ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE inventory_sessions SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- InventoryItems
    EXECUTE 'ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE inventory_items SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- PendingScans
    EXECUTE 'ALTER TABLE pending_scans ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE pending_scans SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- AuditLogs
    EXECUTE 'ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS warehouse_id BIGINT';
    EXECUTE 'UPDATE audit_logs SET warehouse_id = ' || default_warehouse_id || ' WHERE warehouse_id IS NULL';

    -- Add foreign keys
    EXECUTE 'ALTER TABLE products ADD CONSTRAINT fk_products_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE locations ADD CONSTRAINT fk_locations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE location_stock ADD CONSTRAINT fk_location_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE batches ADD CONSTRAINT fk_batches_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE stock_reservations ADD CONSTRAINT fk_stock_reservations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE warehouse_documents ADD CONSTRAINT fk_warehouse_documents_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE warehouse_document_items ADD CONSTRAINT fk_warehouse_doc_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE inventory_sessions ADD CONSTRAINT fk_inventory_sessions_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE inventory_items ADD CONSTRAINT fk_inventory_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE pending_scans ADD CONSTRAINT fk_pending_scans_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
    EXECUTE 'ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)';
END $$;
