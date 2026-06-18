#!/bin/bash
# Check KSeF health status via Actuator
# Usage: ./ksef-health-check.sh

set -e

API_BASE="${KSEF_API_URL:-http://localhost:8080}"

echo "═══ KSeF Health Check ═══"
echo ""

# General health
echo "→ General health:"
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH_STATUS" = "200" ]; then
    echo "  ✅ API is healthy (HTTP 200)"
else
    echo "  ❌ API health check failed (HTTP $HEALTH_STATUS)"
fi

# KSeF health indicator
echo ""
echo "→ KSeF health:"
curl -s "${API_BASE}/actuator/health/ksef" 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  (KSeF health indicator not available)"

# Prometheus metrics
echo ""
echo "→ KSeF Prometheus metrics:"
curl -s "${API_BASE}/actuator/prometheus" 2>/dev/null | grep -i "ksef_" || echo "  (no KSeF metrics found)"

echo ""
echo "═══ Done ═══"
