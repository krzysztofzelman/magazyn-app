-- ============================================================
-- Fix PostgreSQL custom enum types vs JPA @Enumerated(STRING)
--
-- Problem: Entities use @Enumerated(EnumType.STRING) which binds
-- as VARCHAR, but columns were defined as custom PG enum types
-- (ksef_invoice_status, ksef_operation_type). PostgreSQL does
-- NOT allow implicit casting from VARCHAR to custom enums,
-- causing:
--   "operator does not exist: ksef_invoice_status = character varying"
--   "column X is of type Y but expression is of type character varying"
--
-- Fix: Change columns to VARCHAR and drop custom enum types.
-- ============================================================

-- 1. Drop default values that depend on custom enum types
ALTER TABLE ksef_invoices ALTER COLUMN status DROP DEFAULT;

-- 2. Fix ksef_invoices.status
ALTER TABLE ksef_invoices ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- 3. Fix ksef_audit_log.operation
ALTER TABLE ksef_audit_log ALTER COLUMN operation TYPE VARCHAR(30) USING operation::text;

-- 4. Drop the custom enum types (no longer needed)
DROP TYPE IF EXISTS ksef_invoice_status;
DROP TYPE IF EXISTS ksef_operation_type;
