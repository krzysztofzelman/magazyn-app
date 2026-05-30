#!/bin/bash
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
  -d '{"username":"admin","password":"REMOVED"}' \
  --resolve magazyn.kzelman.pl:443:127.0.0.1

echo
echo "=== Check Docker logs for any 403 ==="
docker logs magazyn-app --tail 30 2>&1 | grep -iE '(403|login|access.denied|forbidden)' | tail -10
