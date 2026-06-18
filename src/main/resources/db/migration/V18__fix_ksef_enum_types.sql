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

-- 1. Fix ksef_invoices.status — drop enum default first, then drop indexes using the column
ALTER TABLE ksef_invoices ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- 2. Fix ksef_audit_log.operation
ALTER TABLE ksef_audit_log ALTER COLUMN operation TYPE VARCHAR(30) USING operation::text;

-- 3. Drop the custom enum types (no longer needed)
DROP TYPE IF EXISTS ksef_invoice_status;
DROP TYPE IF EXISTS ksef_operation_type;
