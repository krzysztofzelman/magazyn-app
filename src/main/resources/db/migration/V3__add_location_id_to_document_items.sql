-- Add location_id to warehouse_document_items for PZ/WZ location scanning
ALTER TABLE warehouse_document_items
    ADD COLUMN IF NOT EXISTS location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_warehouse_doc_items_location_id
    ON warehouse_document_items(location_id);
