-- ============================================================
-- V14: Hash refresh tokens
-- ============================================================
-- This migration converts plain-text refresh tokens to SHA-256 hashes.
-- Since V1 already created the table with token_hash column,
-- we only need to ensure the column exists and is NOT NULL.

-- Add token_hash column if it doesn't exist (idempotent)
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);

-- Make NOT NULL (safe: column already has values from V1)
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;

-- Unique index on hash (replaces the old unique constraint on token column)
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Drop old unique constraint if it exists (from older schema where the
-- constraint was named refresh_tokens_token_key instead of using the
-- UNIQUE keyword inline in CREATE TABLE)
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

-- Drop old token column if it exists (from older schema)
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS token;
