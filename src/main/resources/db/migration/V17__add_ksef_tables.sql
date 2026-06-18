-- ============================================================
-- KSeF (Krajowy System e-Faktur) integration tables
-- Based on MF API v1.6 specification (2024-12-20)
-- ============================================================

-- 1. KSeF sessions — authentication tokens for KSeF API
CREATE TABLE ksef_sessions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_token   VARCHAR(512) NOT NULL,
    reference_number VARCHAR(64),
    initiated_by    VARCHAR(100) NOT NULL,
    initiated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL,
    refreshed_at    TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMP,
    error_message   TEXT,
    nip             VARCHAR(20) NOT NULL,
    api_version     VARCHAR(10) DEFAULT '1.6',
    -- Metadata
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ksef_sessions_tenant ON ksef_sessions(tenant_id);
CREATE INDEX idx_ksef_sessions_active ON ksef_sessions(tenant_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_ksef_sessions_expires ON ksef_sessions(expires_at) WHERE is_active = TRUE;

-- 2. KSeF invoices — mapping between internal invoices and KSeF
CREATE TYPE ksef_invoice_status AS ENUM (
    'PENDING',
    'SENT',
    'PROCESSED',
    'ACCEPTED',
    'REJECTED',
    'ERROR',
    'CORRECTED',
    'CANCELLED'
);

CREATE TABLE ksef_invoices (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id          BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    invoice_number      VARCHAR(100) NOT NULL,
    -- KSeF identifiers
    ksef_reference_number VARCHAR(100),
    ksef_timestamp       TIMESTAMP,
    ksef_status_code     VARCHAR(20),
    ksef_status_message  TEXT,
    -- Invoice data (encoded UBL/FA_VAT XML)
    invoice_xml         TEXT,
    invoice_hash        VARCHAR(128),
    response_xml        TEXT,
    -- Status tracking
    status              ksef_invoice_status NOT NULL DEFAULT 'PENDING',
    submission_attempts INTEGER NOT NULL DEFAULT 0,
    last_submitted_at   TIMESTAMP,
    last_error_message  TEXT,
    last_error_code     VARCHAR(50),
    -- Correction tracking
    corrected_by_invoice_id BIGINT REFERENCES ksef_invoices(id) ON DELETE SET NULL,
    -- Metadata
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_ksef_invoices_tenant ON ksef_invoices(tenant_id);
CREATE INDEX idx_ksef_invoices_status ON ksef_invoices(tenant_id, status);
CREATE INDEX idx_ksef_invoices_invoice ON ksef_invoices(invoice_id);
CREATE INDEX idx_ksef_invoices_ksef_ref ON ksef_invoices(ksef_reference_number);
CREATE INDEX idx_ksef_invoices_number ON ksef_invoices(invoice_number);

-- 3. KSeF API responses log — raw responses from KSeF API
CREATE TABLE ksef_responses (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    ksef_invoice_id BIGINT REFERENCES ksef_invoices(id) ON DELETE CASCADE,
    session_id      BIGINT REFERENCES ksef_sessions(id) ON DELETE SET NULL,
    -- Request info
    request_endpoint VARCHAR(255) NOT NULL,
    request_method  VARCHAR(10) NOT NULL,
    request_body    TEXT,
    -- Response info
    response_status INTEGER NOT NULL,
    response_body   TEXT,
    response_time_ms INTEGER,
    -- Error handling
    is_error        BOOLEAN NOT NULL DEFAULT FALSE,
    error_category  VARCHAR(50),
    -- Metadata
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ksef_responses_tenant ON ksef_responses(tenant_id);
CREATE INDEX idx_ksef_responses_invoice ON ksef_responses(ksef_invoice_id);
CREATE INDEX idx_ksef_responses_error ON ksef_responses(tenant_id, is_error) WHERE is_error = TRUE;

-- 4. KSeF audit log — detailed operation log
CREATE TYPE ksef_operation_type AS ENUM (
    'SESSION_INIT',
    'SESSION_REFRESH',
    'SESSION_CLOSE',
    'INVOICE_SEND',
    'INVOICE_STATUS',
    'INVOICE_GET',
    'INVOICE_CORRECT',
    'INVOICE_CANCEL',
    'API_ERROR',
    'VALIDATION_ERROR',
    'RETRY_SUCCESS',
    'RETRY_FAILURE',
    'RATE_LIMIT_HIT',
    'AUTH_ERROR',
    'CONFIG_CHANGE'
);

CREATE TABLE ksef_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    operation       ksef_operation_type NOT NULL,
    -- Related entities
    ksef_invoice_id BIGINT REFERENCES ksef_invoices(id) ON DELETE SET NULL,
    session_id      BIGINT REFERENCES ksef_sessions(id) ON DELETE SET NULL,
    -- Who & what
    performed_by    VARCHAR(100) NOT NULL,
    nip             VARCHAR(20),
    details         TEXT,
    -- Result
    success         BOOLEAN NOT NULL DEFAULT TRUE,
    error_message   TEXT,
    error_code      VARCHAR(50),
    -- Performance
    duration_ms     INTEGER,
    -- Metadata
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ksef_audit_tenant ON ksef_audit_log(tenant_id);
CREATE INDEX idx_ksef_audit_operation ON ksef_audit_log(tenant_id, operation);
CREATE INDEX idx_ksef_audit_invoice ON ksef_audit_log(ksef_invoice_id);
CREATE INDEX idx_ksef_audit_created ON ksef_audit_log(tenant_id, created_at DESC);

-- Trigger function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_ksef_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ksef_sessions_updated
    BEFORE UPDATE ON ksef_sessions
    FOR EACH ROW EXECUTE FUNCTION update_ksef_updated_at();

CREATE TRIGGER trg_ksef_invoices_updated
    BEFORE UPDATE ON ksef_invoices
    FOR EACH ROW EXECUTE FUNCTION update_ksef_updated_at();

-- Add ksef_status column to existing invoices table for KSeF tracking
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ksef_status VARCHAR(30);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ksef_reference_number VARCHAR(100);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ksef_sent_at TIMESTAMP;
