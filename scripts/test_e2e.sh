#!/bin/bash
# End-to-end test: login + authenticated API call
# Usage: TEST_USERNAME=admin TEST_PASSWORD=<haslo> ./scripts/test_e2e.sh
set -e

: "${TEST_USERNAME:?Must set TEST_USERNAME}"
: "${TEST_PASSWORD:?Must set TEST_PASSWORD}"

RESP=$(curl -sk -X POST 'https://magazyn.kzelman.pl/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"$TEST_PASSWORD\"}" \
  --resolve magazyn.kzelman.pl:443:127.0.0.1)

echo "Login response: $RESP" | head -c 100
echo

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "Token obtained: ${TOKEN:0:20}..."

DOCS_RESP=$(curl -sk -w '\nHTTP_CODE:%{http_code}' \
  'https://magazyn.kzelman.pl/api/documents?page=0&size=10' \
  -H "Authorization: Bearer $TOKEN" \
  --resolve magazyn.kzelman.pl:443:127.0.0.1)

echo "$DOCS_RESP" | tail -1
