CREATE TABLE IF NOT EXISTS pending_scans (
    id          BIGSERIAL    PRIMARY KEY,
    mode        VARCHAR(20)  NOT NULL,
    barcode     VARCHAR(255) NOT NULL,
    product_id  BIGINT       NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_sku  VARCHAR(255) NOT NULL,
    product_unit VARCHAR(50)  NOT NULL,
    quantity    INTEGER      NOT NULL DEFAULT 1,
    scanned_by  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pending_scans_mode ON pending_scans(mode);
CREATE INDEX IF NOT EXISTS idx_pending_scans_scanned_by ON pending_scans(scanned_by);
