-- ============================================================
-- V15: Add `used` column to refresh_tokens for token rotation
-- ============================================================
-- This column tracks whether a refresh token has already been
-- consumed by a rotation. It enables reuse detection: if an
-- already-used token is presented again, all tokens for that
-- user are invalidated (stolen-token protection).
-- ============================================================

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS used BOOLEAN NOT NULL DEFAULT FALSE;
