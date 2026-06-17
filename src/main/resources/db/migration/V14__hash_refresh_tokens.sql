-- Store SHA-256 hashes of refresh tokens instead of plaintext UUIDs
-- This prevents token theft if the database is compromised.

-- Add token_hash column
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);

-- Backfill is skipped when there was never a separate 'token' column
-- (Hibernate ddl-auto creates the table with token_hash only).
-- On databases where a 'token' column exists, the backfill is handled by
-- the application service layer during the first migration.

-- Make NOT NULL (safe: column already has values from Hibernate-generated schema)
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;

-- Unique index on hash (replaces the old unique constraint on the token column)
DROP INDEX IF EXISTS refresh_tokens_token_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Drop old token column (no longer needed; lookups use token_hash)
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS token;
