#!/bin/bash
# Pre-commit hook: block commits containing secrets / hardcoded credentials
#
# Install: copy to .git/hooks/pre-commit (or run scripts/install-hooks.sh)
# Skip:   git commit --no-verify  (use ONLY for legitimate non-secret changes)

set -e

RED='\033[0;31m'
NC='\033[0m' # No Color

# ─────────────────────────────────────────────────────────────────
# Patterns that MUST NOT appear in staged files
# ─────────────────────────────────────────────────────────────────

# Hardcoded password-like values (exclude ${...} env-var patterns)
PASSWORD_PATTERNS=(
    # Database credentials in code (not env vars)
    'password\s*=\s*"([^"$]*)"'
    'password\s*:\s*.([^$\n]*).'
    'POSTGRES_PASSWORD:\s*[^$]'
    'spring\.datasource\.password\s*=\s*[^$]'

    # IP addresses in config files (not in comments/docs)
    '(host|server_name|VPS_HOST).*\b([0-9]{1,3}\.){3}[0-9]{1,3}\b'

    # Known weak/default passwords
    'admin123'
    'P@ssw0rd'
    'password123'
    'test123'
)

# Files that should NEVER be committed
FORBIDDEN_FILES=(
    '.env'
    'gen_env.py'
)

# ─────────────────────────────────────────────────────────────────
# Check staged files
# ─────────────────────────────────────────────────────────────────

STAGED_FILES=$(git diff --cached --name-only)
STAGED_DIFF=$(git diff --cached)

HAS_ERROR=false

# Check forbidden files
for FILE in "${FORBIDDEN_FILES[@]}"; do
    if echo "$STAGED_FILES" | grep -q "^$FILE$"; then
        echo -e "${RED}[SECURITY]⛔ Forbidden file staged: $FILE${NC}"
        echo "         This file contains secrets and must NOT be committed."
        echo "         Remove it with: git rm --cached $FILE (if tracked)"
        echo "         Or add to .gitignore and unstage."
        HAS_ERROR=true
    fi
done

# Check password patterns in staged diff
for PATTERN in "${PASSWORD_PATTERNS[@]}"; do
    if echo "$STAGED_DIFF" | grep -Pq "$PATTERN"; then
        MATCHES=$(echo "$STAGED_DIFF" | grep -Pn "$PATTERN" | head -5)
        echo -e "${RED}[SECURITY]⛔ Possible secret detected in staged changes:${NC}"
        echo "         Pattern: $PATTERN"
        echo "         Matches:"
        echo "$MATCHES" | while IFS= read -r line; do
            echo "           $line"
        done
        HAS_ERROR=true
    fi
done

if [ "$HAS_ERROR" = true ]; then
    echo ""
    echo -e "${RED}Commit blocked. Use git commit --no-verify to bypass (only if you're SURE no secrets).${NC}"
    exit 1
fi

exit 0
