#!/bin/bash
# ============================================================
# Flyway repair + baseline script for existing production DB
#
# Run this ONCE on an existing database that already has
# V2–V14 applied (e.g. the VPS deployment) AFTER adding
# V1__init.sql to the migration directory.
#
# What this does:
#   1. flyway repair  — removes failed migration entries and
#      recalculates checksums for already-applied migrations
#   2. flyway baseline — marks V1 as baseline (already applied
#      via existing schema) so Flyway does not try to re-apply it
# ============================================================

set -euo pipefail

# ── Configuration (adjust to match your environment) ──────────
DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/magazyn_db}"
DB_USER="${DB_USERNAME:-magazyn}"
DB_PASS="${DB_PASSWORD:-}"
FLYWAY_LOCATIONS="${FLYWAY_LOCATIONS:-filesystem:src/main/resources/db/migration}"

echo "==> Running flyway repair…"
flyway \
  -url="${DB_URL}" \
  -user="${DB_USER}" \
  -password="${DB_PASS}" \
  -locations="${FLYWAY_LOCATIONS}" \
  repair

echo "==> Running flyway baseline (version=1)…"
flyway \
  -url="${DB_URL}" \
  -user="${DB_USER}" \
  -password="${DB_PASS}" \
  -locations="${FLYWAY_LOCATIONS}" \
  -baselineVersion=1 \
  baseline

echo "==> Done!  Flyway will now skip V1 (baseline) and treat"
echo "    V2–V14 as already applied."
echo ""
echo "    On next app restart, Spring Boot (with"
echo "    baseline-on-migrate=true, baseline-version=1) will"
echo "    automatically baseline and migrate."
