-- Add warehouse_id to invoices and invoice_items for warehouse-level isolation
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_invoices_warehouse ON invoices(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_warehouse ON invoice_items(warehouse_id);
