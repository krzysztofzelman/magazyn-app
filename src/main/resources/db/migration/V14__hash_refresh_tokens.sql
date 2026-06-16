-- Store SHA-256 hashes of refresh tokens instead of plaintext UUIDs
-- This prevents token theft if the database is compromised.

-- Add token_hash column
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);

-- Backfill existing UUID tokens using PostgreSQL's built-in sha256
-- sha256(bytea) returns bytea; encode(..., 'hex') gives hex string
UPDATE refresh_tokens
SET token_hash = encode(sha256(convert_to(token::text, 'UTF8')), 'hex')
WHERE token_hash IS NULL;

-- Make NOT NULL after backfill (safe because all existing rows have UUID tokens)
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;

-- Unique index on hash (replaces the old unique constraint on the token column)
DROP INDEX IF EXISTS refresh_tokens_token_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Drop old token column (no longer needed; lookups use token_hash)
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS token;
