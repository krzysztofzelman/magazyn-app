-- Invoice system
-- Adds tables for automatic invoicing on WZ confirmation

-- Seller info stored as app-level configuration
CREATE TABLE IF NOT EXISTS company_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(20),
    address TEXT,
    bank_name VARCHAR(255),
    bank_account VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Invoice headers
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(50) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    document_id BIGINT REFERENCES warehouse_documents(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',

    -- Seller (from company settings at time of issue)
    seller_name VARCHAR(255) NOT NULL,
    seller_tax_id VARCHAR(20),
    seller_address TEXT,
    seller_bank_account VARCHAR(50),

    -- Buyer (from contractor at time of issue)
    buyer_name VARCHAR(255) NOT NULL,
    buyer_tax_id VARCHAR(20),
    buyer_address TEXT,

    -- Dates
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    sale_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,

    -- Payment
    payment_method VARCHAR(20) DEFAULT 'PRZELEW',
    payment_account VARCHAR(50),

    -- Totals (computed)
    total_net NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_vat NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_gross NUMERIC(12, 2) NOT NULL DEFAULT 0,

    -- Metadata
    notes TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_document ON invoices(document_id);

-- Invoice line items
CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL,

    -- Product info (snapshot at time of invoice)
    product_id BIGINT REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(255),
    product_unit VARCHAR(50),

    -- Pricing
    quantity INTEGER NOT NULL,
    unit_price_net NUMERIC(10, 2) NOT NULL DEFAULT 0,
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 23.00,
    vat_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_net NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_gross NUMERIC(12, 2) NOT NULL DEFAULT 0,

    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_tenant ON invoice_items(tenant_id);

-- Add invoicing fields to contractors
ALTER TABLE contractors ADD COLUMN IF NOT EXISTS bank_account VARCHAR(50);
ALTER TABLE contractors ADD COLUMN IF NOT EXISTS payment_days INTEGER DEFAULT 14;
ALTER TABLE contractors ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'PRZELEW';

-- Add default VAT rate to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS default_vat_rate NUMERIC(5, 2) DEFAULT 23.00;

-- Set defaults for existing data
UPDATE products SET default_vat_rate = 23.00 WHERE default_vat_rate IS NULL;
UPDATE contractors SET payment_days = 14 WHERE payment_days IS NULL;
UPDATE contractors SET payment_method = 'PRZELEW' WHERE payment_method IS NULL;
