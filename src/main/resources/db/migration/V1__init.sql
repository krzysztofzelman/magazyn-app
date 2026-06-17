-- ============================================================
-- V1__init.sql — Complete initial schema for the magazyn system
--
-- This migration creates ALL tables with their FINAL column set
-- (after all V2–V14 migrations), so a fresh database is fully
-- operational after just V1.  Later migrations (V2+) are still
-- applied and are mostly no-ops thanks to IF NOT EXISTS guards.
--
-- Safe for existing databases:
--   - CREATE TABLE IF NOT EXISTS  → no-op if table exists
--   - CREATE INDEX IF NOT EXISTS   → no-op if index exists
--   - UNIQUE constraints on CREATE → safe (new table only)
--
-- Foreign keys are intentionally omitted here — they are created
-- by the V11 migration (which uses dynamic SQL + IF NOT EXISTS
-- for every constraint) to stay compatible with the existing
-- migration chain.
-- ============================================================

-- ============================================================
-- 1. tenants
-- ============================================================
CREATE TABLE IF NOT EXISTS tenants (
    id          BIGSERIAL       PRIMARY KEY,
    subdomain   VARCHAR(255)    NOT NULL UNIQUE,
    name        VARCHAR(255)    NOT NULL,
    api_key     VARCHAR(255)    NOT NULL UNIQUE,
    plan        VARCHAR(50)     NOT NULL DEFAULT 'free',
    max_users   INTEGER         NOT NULL DEFAULT 3,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- ============================================================
-- 2. users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(255)    NOT NULL,
    email       VARCHAR(255),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    tenant_id   BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- ============================================================
-- 3. products
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL,
    sku                 VARCHAR(255)    NOT NULL UNIQUE,
    description         TEXT,
    unit                VARCHAR(255)    NOT NULL,
    quantity            INTEGER         NOT NULL DEFAULT 0,
    price               DECIMAL(10,2),
    min_quantity        INTEGER         DEFAULT 0,
    barcode             VARCHAR(255)    UNIQUE,
    track_expiry        BOOLEAN         NOT NULL DEFAULT FALSE,
    location_id         BIGINT,
    category_id         BIGINT,
    default_location_id BIGINT,
    default_vat_rate    DECIMAL(5,2)    DEFAULT 23.00,
    warehouse_id        BIGINT,
    created_at          TIMESTAMP,
    version             INTEGER         NOT NULL DEFAULT 0,
    tenant_id           BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_warehouse_id ON products(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode);

-- ============================================================
-- 4. locations
-- ============================================================
CREATE TABLE IF NOT EXISTS locations (
    id          BIGSERIAL       PRIMARY KEY,
    code        VARCHAR(255)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    type        VARCHAR(255)    NOT NULL,
    parent_id   BIGINT,
    description TEXT,
    barcode     VARCHAR(100)    UNIQUE,
    qr_data     TEXT,
    capacity    INTEGER,
    occupied    INTEGER         DEFAULT 0,
    zone        VARCHAR(50),
    is_active   BOOLEAN         DEFAULT TRUE,
    rack        VARCHAR(50),
    shelf       VARCHAR(50),
    warehouse_id BIGINT,
    tenant_id   BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_locations_tenant_id ON locations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_locations_warehouse_id ON locations(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_locations_parent_id ON locations(parent_id);
CREATE INDEX IF NOT EXISTS idx_locations_barcode ON locations(barcode);

-- ============================================================
-- 5. location_stock
-- ============================================================
CREATE TABLE IF NOT EXISTS location_stock (
    id                BIGSERIAL        PRIMARY KEY,
    location_id       BIGINT           NOT NULL,
    product_id        BIGINT           NOT NULL,
    quantity          DECIMAL(15,2)    NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(15,2)    NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           INTEGER          NOT NULL DEFAULT 0,
    warehouse_id      BIGINT,
    tenant_id         BIGINT           NOT NULL,
    UNIQUE(location_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_location_stock_location_id ON location_stock(location_id);
CREATE INDEX IF NOT EXISTS idx_location_stock_product_id ON location_stock(product_id);
CREATE INDEX IF NOT EXISTS idx_location_stock_tenant_id ON location_stock(tenant_id);
CREATE INDEX IF NOT EXISTS idx_location_stock_warehouse_id ON location_stock(warehouse_id);

-- ============================================================
-- 6. stock_movements
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_movements (
    id          BIGSERIAL       PRIMARY KEY,
    product_id  BIGINT          NOT NULL,
    type        VARCHAR(255)    NOT NULL,
    quantity    INTEGER         NOT NULL,
    note        VARCHAR(255),
    created_at  TIMESTAMP,
    created_by  VARCHAR(255)    NOT NULL,
    batch_id    BIGINT,
    warehouse_id BIGINT,
    tenant_id   BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_id ON stock_movements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_warehouse_id ON stock_movements(warehouse_id);

-- ============================================================
-- 7. stock_reservations
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_reservations (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    quantity        INTEGER         NOT NULL,
    reference_type  VARCHAR(255)    NOT NULL,
    reference_id    VARCHAR(255),
    status          VARCHAR(255)    NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP,
    expires_at      TIMESTAMP,
    notes           TEXT,
    warehouse_id    BIGINT,
    version         INTEGER         NOT NULL DEFAULT 0,
    tenant_id       BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_reservations_tenant_id ON stock_reservations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_reservations_warehouse_id ON stock_reservations(warehouse_id);

-- ============================================================
-- 8. warehouses
-- ============================================================
CREATE TABLE IF NOT EXISTS warehouses (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    code        VARCHAR(255)    NOT NULL,
    is_active   BOOLEAN         DEFAULT TRUE,
    tenant_id   BIGINT          NOT NULL,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_warehouses_tenant_id ON warehouses(tenant_id);

-- ============================================================
-- 9. warehouse_documents
-- ============================================================
CREATE TABLE IF NOT EXISTS warehouse_documents (
    id            BIGSERIAL       PRIMARY KEY,
    number        VARCHAR(255)    NOT NULL UNIQUE,
    type          VARCHAR(2)      NOT NULL,
    contractor_id BIGINT          NOT NULL,
    status        VARCHAR(255)    NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMP       NOT NULL,
    confirmed_at  TIMESTAMP,
    created_by    VARCHAR(255)    NOT NULL,
    notes         TEXT,
    warehouse_id  BIGINT,
    version       INTEGER         NOT NULL DEFAULT 0,
    tenant_id     BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_warehouse_documents_tenant_id ON warehouse_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_documents_warehouse_id ON warehouse_documents(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_documents_contractor_id ON warehouse_documents(contractor_id);

-- ============================================================
-- 10. warehouse_document_items
-- ============================================================
CREATE TABLE IF NOT EXISTS warehouse_document_items (
    id                BIGSERIAL        PRIMARY KEY,
    document_id       BIGINT           NOT NULL,
    product_id        BIGINT           NOT NULL,
    quantity          INTEGER          NOT NULL,
    unit_price        DECIMAL(10,2)    DEFAULT 0,
    lot_number        VARCHAR(255),
    expiry_date       DATE,
    manufacturing_date DATE,
    location_id       BIGINT,
    warehouse_id      BIGINT,
    version           INTEGER          NOT NULL DEFAULT 0,
    tenant_id         BIGINT           NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_document_id ON warehouse_document_items(document_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_product_id ON warehouse_document_items(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_tenant_id ON warehouse_document_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_warehouse_id ON warehouse_document_items(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_location_id ON warehouse_document_items(location_id);

-- ============================================================
-- 11. audit_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(255)    NOT NULL,
    action      VARCHAR(255)    NOT NULL,
    entity_type VARCHAR(255),
    entity_id   BIGINT,
    details     TEXT,
    ip_address  VARCHAR(255),
    timestamp   TIMESTAMP,
    warehouse_id BIGINT,
    tenant_id   BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_warehouse_id ON audit_logs(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_username ON audit_logs(username);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);

-- ============================================================
-- 12. batches
-- ============================================================
CREATE TABLE IF NOT EXISTS batches (
    id                BIGSERIAL       PRIMARY KEY,
    product_id        BIGINT          NOT NULL,
    lot_number        VARCHAR(255)    NOT NULL,
    expiry_date       DATE,
    manufacturing_date DATE,
    quantity          INTEGER         NOT NULL,
    location_id       BIGINT,
    warehouse_id      BIGINT,
    created_at        TIMESTAMP,
    version           INTEGER         NOT NULL DEFAULT 0,
    tenant_id         BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_batch_product_expiry ON batches(product_id, expiry_date);
CREATE INDEX IF NOT EXISTS idx_batch_product_created ON batches(product_id, created_at);
CREATE INDEX IF NOT EXISTS idx_batches_tenant_id ON batches(tenant_id);
CREATE INDEX IF NOT EXISTS idx_batches_warehouse_id ON batches(warehouse_id);

-- ============================================================
-- 13. contractors
-- ============================================================
CREATE TABLE IF NOT EXISTS contractors (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    tax_id          VARCHAR(255)    UNIQUE,
    address         VARCHAR(255),
    email           VARCHAR(255),
    phone           VARCHAR(255),
    type            VARCHAR(255)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    bank_account    VARCHAR(50),
    payment_days    INTEGER         DEFAULT 14,
    payment_method  VARCHAR(20)     DEFAULT 'PRZELEW',
    created_at      TIMESTAMP       NOT NULL,
    version         INTEGER         NOT NULL DEFAULT 0,
    tenant_id       BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_contractors_tenant_id ON contractors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contractors_name ON contractors(name);

-- ============================================================
-- 14. inventory_sessions
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_sessions (
    id            BIGSERIAL       PRIMARY KEY,
    name          VARCHAR(255)    NOT NULL,
    created_at    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(100),
    status        VARCHAR(20)     DEFAULT 'OPEN',
    warehouse_id  BIGINT,
    tenant_id     BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_inventory_sessions_tenant_id ON inventory_sessions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_sessions_warehouse_id ON inventory_sessions(warehouse_id);

-- ============================================================
-- 15. inventory_items
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_items (
    id                BIGSERIAL        PRIMARY KEY,
    session_id        BIGINT           NOT NULL,
    location_id       BIGINT,
    product_id        BIGINT           NOT NULL,
    expected_quantity DECIMAL(15,2)    DEFAULT 0,
    counted_quantity  DECIMAL(15,2),
    scanned_at        TIMESTAMP,
    scanned_by        VARCHAR(100),
    warehouse_id      BIGINT,
    tenant_id         BIGINT           NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_inventory_items_session_id ON inventory_items(session_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_location_id ON inventory_items(location_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_product_id ON inventory_items(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_tenant_id ON inventory_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_warehouse_id ON inventory_items(warehouse_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_items_session_location_product
    ON inventory_items(session_id, location_id, product_id);

-- ============================================================
-- 16. pending_scans
-- ============================================================
CREATE TABLE IF NOT EXISTS pending_scans (
    id           BIGSERIAL       PRIMARY KEY,
    mode         VARCHAR(20)     NOT NULL,
    barcode      VARCHAR(255)    NOT NULL,
    product_id   BIGINT          NOT NULL,
    product_name VARCHAR(255)    NOT NULL,
    product_sku  VARCHAR(255)    NOT NULL,
    product_unit VARCHAR(50)     NOT NULL,
    quantity     INTEGER         NOT NULL DEFAULT 1,
    scanned_by   VARCHAR(100)    NOT NULL,
    created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    warehouse_id BIGINT,
    tenant_id    BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pending_scans_mode ON pending_scans(mode);
CREATE INDEX IF NOT EXISTS idx_pending_scans_scanned_by ON pending_scans(scanned_by);
CREATE INDEX IF NOT EXISTS idx_pending_scans_tenant_id ON pending_scans(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pending_scans_warehouse_id ON pending_scans(warehouse_id);

-- ============================================================
-- 17. refresh_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    token_hash  VARCHAR(64)     NOT NULL UNIQUE,
    user_id     BIGINT          NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    tenant_id   BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_tenant_id ON refresh_tokens(tenant_id);

-- ============================================================
-- 18. company_settings
-- ============================================================
CREATE TABLE IF NOT EXISTS company_settings (
    id            BIGSERIAL       PRIMARY KEY,
    name          VARCHAR(255)    NOT NULL,
    tax_id        VARCHAR(20),
    address       TEXT,
    bank_name     VARCHAR(255),
    bank_account  VARCHAR(50),
    phone         VARCHAR(50),
    email         VARCHAR(255),
    updated_at    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    tenant_id     BIGINT          NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_company_settings_tenant_id ON company_settings(tenant_id);

-- ============================================================
-- 19. invoices
-- ============================================================
CREATE TABLE IF NOT EXISTS invoices (
    id                BIGSERIAL        PRIMARY KEY,
    number            VARCHAR(50)      NOT NULL UNIQUE,
    tenant_id         BIGINT           NOT NULL,
    document_id       BIGINT,
    status            VARCHAR(20)      NOT NULL DEFAULT 'ISSUED',
    seller_name       VARCHAR(255)     NOT NULL,
    seller_tax_id     VARCHAR(20),
    seller_address    TEXT,
    seller_bank_account VARCHAR(50),
    buyer_name        VARCHAR(255)     NOT NULL,
    buyer_tax_id      VARCHAR(20),
    buyer_address     TEXT,
    issue_date        DATE             NOT NULL,
    sale_date         DATE             NOT NULL,
    due_date          DATE,
    payment_method    VARCHAR(20)      DEFAULT 'PRZELEW',
    payment_account   VARCHAR(50),
    total_net         NUMERIC(12,2)    NOT NULL DEFAULT 0,
    total_vat         NUMERIC(12,2)    NOT NULL DEFAULT 0,
    total_gross       NUMERIC(12,2)    NOT NULL DEFAULT 0,
    notes             TEXT,
    created_by        VARCHAR(255)     NOT NULL,
    created_at        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at           TIMESTAMP,
    cancelled_at      TIMESTAMP,
    version           INTEGER          NOT NULL DEFAULT 0,
    warehouse_id      BIGINT
);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_document ON invoices(document_id);
CREATE INDEX IF NOT EXISTS idx_invoices_warehouse ON invoices(warehouse_id);

-- ============================================================
-- 20. invoice_items
-- ============================================================
CREATE TABLE IF NOT EXISTS invoice_items (
    id              BIGSERIAL        PRIMARY KEY,
    invoice_id      BIGINT           NOT NULL,
    tenant_id       BIGINT           NOT NULL,
    product_id      BIGINT,
    product_name    VARCHAR(255)     NOT NULL,
    product_sku     VARCHAR(255),
    product_unit    VARCHAR(50),
    quantity        INTEGER          NOT NULL,
    unit_price_net  NUMERIC(10,2)    NOT NULL DEFAULT 0,
    vat_rate        NUMERIC(5,2)     NOT NULL DEFAULT 23.00,
    vat_amount      NUMERIC(12,2)    NOT NULL DEFAULT 0,
    total_net       NUMERIC(12,2)    NOT NULL DEFAULT 0,
    total_gross     NUMERIC(12,2)    NOT NULL DEFAULT 0,
    version         INTEGER          NOT NULL DEFAULT 0,
    warehouse_id    BIGINT
);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_tenant ON invoice_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_warehouse ON invoice_items(warehouse_id);
