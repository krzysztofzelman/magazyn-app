#!/bin/bash
# Login test script
# Usage: TEST_USERNAME=admin TEST_PASSWORD=<haslo> ./scripts/test_login_details.sh

: "${TEST_USERNAME:?Must set TEST_USERNAME}"
: "${TEST_PASSWORD:?Must set TEST_PASSWORD}"

echo "=== OPTIONS preflight ==="
curl -sk -w '\nHTTP_CODE:%{http_code}' -X OPTIONS 'https://magazyn.kzelman.pl/api/auth/login' \
  -H 'Origin: https://magazyn.kzelman.pl' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type' \
  --resolve magazyn.kzelman.pl:443:127.0.0.1

echo
echo "=== POST with Origin + content-type (simulating browser) ==="
curl -sk -w '\nHTTP_CODE:%{http_code}' -X POST 'https://magazyn.kzelman.pl/api/auth/login' \
  -H 'Origin: https://magazyn.kzelman.pl' \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"$TEST_PASSWORD\"}" \
  --resolve magazyn.kzelman.pl:443:127.0.0.1

echo
echo "=== Check Docker logs for any 403 ==="
docker logs magazyn-app --tail 30 2>&1 | grep -iE '(403|login|access.denied|forbidden)' | tail -10
