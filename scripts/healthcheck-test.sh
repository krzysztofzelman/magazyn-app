#!/bin/bash
#
# healthcheck-test.sh
# Tests Docker container healthcheck in CI or locally.
#
# Usage:
#   ./scripts/healthcheck-test.sh                    # uses existing image magazyn-app:latest
#   ./scripts/healthcheck-test.sh --build            # builds image first
#   ./scripts/healthcheck-test.sh --image myapp:tag  # uses specific image
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
BOLD='\033[1m'

pass() { echo -e "${GREEN}✅${NC} $1"; }
fail() { echo -e "${RED}❌${NC} $1"; }
info() { echo -e "${YELLOW}ℹ️${NC} $1"; }
header() { echo -e "\n${BOLD}═══ $1 ═══${NC}\n"; }

# ─── Parse arguments ───
IMAGE="magazyn-app:latest"
DO_BUILD=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --build) DO_BUILD=true; shift ;;
        --image) IMAGE="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ─── Build image if requested ───
if [ "$DO_BUILD" = true ]; then
    header "Building Docker image"
    docker build -t "$IMAGE" "$PROJECT_DIR"
    pass "Image built: $IMAGE"
fi

# ─── Check image exists ───
header "Checking image"
docker image inspect "$IMAGE" >/dev/null 2>&1 || {
    fail "Image $IMAGE not found. Build it first or use --build"
    exit 1
}
pass "Image $IMAGE found"

# ─── Start container ───
header "Starting container"
CONTAINER_NAME="healthcheck-test-$$"

docker run -d \
    --name "$CONTAINER_NAME" \
    -p 8080:8080 \
    -e DB_URL=jdbc:postgresql://host.docker.internal:5432/magazyn_test \
    -e DB_USERNAME=test_user \
    -e DB_PASSWORD=test_password \
    -e JWT_SECRET=test-jwt-secret-for-ci-$(date +%s) \
    -e JWT_EXPIRATION=900000 \
    -e MANAGEMENT_HEALTH_MAIL_ENABLED=false \
    -e NOTIFICATIONS_ENABLED=false \
    "$IMAGE"

PASS=false
cleanup() {
    info "Cleaning up container $CONTAINER_NAME..."
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
    docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true
    if [ "$PASS" = false ]; then
        fail "Healthcheck test FAILED"
        exit 1
    fi
    pass "Healthcheck test PASSED"
}
trap cleanup EXIT

# ─── Wait for health status ───
header "Waiting for healthy status (up to 3 min)"
MAX_WAIT=180
SLEEP_INTERVAL=10
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    HEALTH=$(docker inspect "$CONTAINER_NAME" --format='{{.State.Health.Status}}' 2>/dev/null || echo "created")
    
    case "$HEALTH" in
        healthy)
            info "  [${ELAPSED}s] ✅ $HEALTH"
            break
            ;;
        unhealthy)
            fail "Container unhealthy at ${ELAPSED}s"
            docker logs "$CONTAINER_NAME" --tail 50
            exit 1
            ;;
        *)
            info "  [${ELAPSED}s] $HEALTH..."
            ;;
    esac
    
    sleep $SLEEP_INTERVAL
    ELAPSED=$((ELAPSED + SLEEP_INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    fail "Timed out after ${MAX_WAIT}s — container never healthy"
    docker logs "$CONTAINER_NAME" --tail 50
    exit 1
fi

# ─── Verify endpoints ───
header "Verifying endpoints"

# Health
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
    pass "/actuator/health → HTTP $HTTP_CODE"
else
    fail "/actuator/health → HTTP $HTTP_CODE"
    exit 1
fi

# Health body
HEALTH_BODY=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null || echo "{}")
if echo "$HEALTH_BODY" | grep -q '"status":"UP"'; then
    pass "Health body: $HEALTH_BODY"
else
    fail "Health body unexpected: $HEALTH_BODY"
    exit 1
fi

# Prometheus metrics (if exposed)
PROM_COUNT=$(curl -sf http://localhost:8080/actuator/prometheus 2>/dev/null | wc -l || echo "0")
if [ "$PROM_COUNT" -gt 0 ] 2>/dev/null; then
    pass "/actuator/prometheus → $PROM_COUNT metrics lines"
else
    warn "/actuator/prometheus → 0 lines (not exposed or behind auth)"
fi

# ─── Done ───
PASS=true
pass "All healthcheck tests passed!"
