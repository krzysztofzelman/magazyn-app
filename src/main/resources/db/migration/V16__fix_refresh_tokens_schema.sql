-- ============================================================
-- V16: Fix refresh_tokens schema (repair for existing databases)
-- ============================================================
-- This migration repairs the refresh_tokens table for databases
-- where V14 was already applied and may have broken the schema.

-- Ensure token_hash column exists and is NOT NULL
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;

-- Drop old token column if it exists
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS token;

-- Ensure used column exists (safe re-application of V15)
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS used BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure unique index exists
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Drop old constraint if it exists (from older schema)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'refresh_tokens_token_key' 
        AND conrelid = 'refresh_tokens'::regclass
    ) THEN
        ALTER TABLE refresh_tokens DROP CONSTRAINT refresh_tokens_token_key;
    END IF;
END $$;
