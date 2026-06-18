#!/bin/bash
# Check KSeF session status
# Usage: ./ksef-session-check.sh [token]

set -e

API_BASE="${KSEF_API_URL:-http://localhost:8080}"

if [ -n "$1" ]; then
    TOKEN="$1"
elif [ -n "$KSEF_TOKEN" ]; then
    TOKEN="$KSEF_TOKEN"
else
    echo "Usage: $0 <jwt-token>"
    echo "Or set KSEF_TOKEN environment variable"
    exit 1
fi

echo "═══ KSeF Session Check ═══"
echo ""

# Get all sessions
echo "→ Active sessions:"
curl -s -X GET "${API_BASE}/api/ksef/admin/sessions" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/json" | python3 -m json.tool 2>/dev/null || echo "  (no active sessions or not ADMIN)"

echo ""
echo "→ Dashboard stats:"
curl -s -X GET "${API_BASE}/api/ksef/admin/dashboard" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/json" | python3 -m json.tool 2>/dev/null || echo "  (unavailable)"

echo ""
echo "═══ Done ═══"
