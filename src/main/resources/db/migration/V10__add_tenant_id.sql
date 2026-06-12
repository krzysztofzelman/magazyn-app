-- Add tenant_id to all tenant-aware tables, backfill with default tenant (id=1)
-- We assume the default "self-hosted" tenant from V9 has id = 1.

-- Helper: get the default tenant ID from the tenants table
-- (will resolve to 1 since it's the first insert, but this
--  approach is more robust across environments with different seed data)

DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id FROM tenants ORDER BY id LIMIT 1;

    -- AuditLog
    EXECUTE 'ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE audit_logs ALTER COLUMN tenant_id DROP DEFAULT';

    -- Batches
    EXECUTE 'ALTER TABLE batches ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE batches ALTER COLUMN tenant_id DROP DEFAULT';

    -- Contractors
    EXECUTE 'ALTER TABLE contractors ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE contractors ALTER COLUMN tenant_id DROP DEFAULT';

    -- InventoryItems
    EXECUTE 'ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE inventory_items ALTER COLUMN tenant_id DROP DEFAULT';

    -- InventorySessions
    EXECUTE 'ALTER TABLE inventory_sessions ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE inventory_sessions ALTER COLUMN tenant_id DROP DEFAULT';

    -- Locations
    EXECUTE 'ALTER TABLE locations ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE locations ALTER COLUMN tenant_id DROP DEFAULT';

    -- LocationStock
    EXECUTE 'ALTER TABLE location_stock ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE location_stock ALTER COLUMN tenant_id DROP DEFAULT';

    -- PendingScans
    EXECUTE 'ALTER TABLE pending_scans ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE pending_scans ALTER COLUMN tenant_id DROP DEFAULT';

    -- Products
    EXECUTE 'ALTER TABLE products ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE products ALTER COLUMN tenant_id DROP DEFAULT';

    -- RefreshTokens
    EXECUTE 'ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE refresh_tokens ALTER COLUMN tenant_id DROP DEFAULT';

    -- StockMovements
    EXECUTE 'ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE stock_movements ALTER COLUMN tenant_id DROP DEFAULT';

    -- StockReservations
    EXECUTE 'ALTER TABLE stock_reservations ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE stock_reservations ALTER COLUMN tenant_id DROP DEFAULT';

    -- Users
    EXECUTE 'ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE users ALTER COLUMN tenant_id DROP DEFAULT';

    -- WarehouseDocuments
    EXECUTE 'ALTER TABLE warehouse_documents ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE warehouse_documents ALTER COLUMN tenant_id DROP DEFAULT';

    -- WarehouseDocumentItems
    EXECUTE 'ALTER TABLE warehouse_document_items ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT ' || default_tenant_id;
    EXECUTE 'ALTER TABLE warehouse_document_items ALTER COLUMN tenant_id DROP DEFAULT';
END $$;
