#!/bin/bash
# Install pre-commit hook for secret detection
# Run from repo root: bash scripts/install-hooks.sh

set -e

HOOK_SRC="scripts/pre-commit.sh"
HOOK_DST=".git/hooks/pre-commit"

if [ ! -f "$HOOK_SRC" ]; then
    echo "Error: $HOOK_SRC not found. Run from repo root."
    exit 1
fi

cp "$HOOK_SRC" "$HOOK_DST"
chmod +x "$HOOK_DST"

echo "✅ Pre-commit hook installed at $HOOK_DST"
echo "   It will now scan every commit for hardcoded secrets."
echo ""
echo "   To bypass (emergency only): git commit --no-verify"
