#!/bin/bash
set -e

echo "═══════════════════════════════════════════"
echo "  Deploying Magazyn App to production"
echo "═══════════════════════════════════════════"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Ensure we're in the right directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# ─── Step 1: Check environment ───
log_info "Checking environment..."

if [ ! -f .env ]; then
    log_error ".env file not found!"
    log_error "Copy .env.example to .env and configure all variables."
    exit 1
fi

source .env

MISSING_VARS=""
[ -z "$DB_PASSWORD" ] && MISSING_VARS="$MISSING_VARS DB_PASSWORD"
[ -z "$JWT_SECRET" ] && MISSING_VARS="$MISSING_VARS JWT_SECRET"
[ -z "$DB_USERNAME" ] && MISSING_VARS="$MISSING_VARS DB_USERNAME"

if [ -n "$MISSING_VARS" ]; then
    log_error "Missing required env variables:$MISSING_VARS"
    exit 1
fi

# ─── Step 2: Pull latest code ───
log_info "Pulling latest code from GitHub..."
git pull origin main

# ─── Step 3: Build backend ───
log_info "Backend build handled by Docker multi-stage build (Dockerfile)"
log_info "Skipping local Maven build — Docker will use maven:3.9-eclipse-temurin-25"

# ─── Step 4: Build frontend ───
log_info "Building frontend..."
FRONTEND_DIR="$PROJECT_DIR/../magazyn-frontend"
if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    npm ci --silent
    npm run build
    log_info "Frontend build complete: dist/"
else
    log_warn "Frontend directory not found at $FRONTEND_DIR"
    log_warn "Skipping frontend build. Make sure to build and deploy separately."
fi

# ─── Step 5: Deploy with Docker Compose ───
log_info "Deploying containers..."
cd "$PROJECT_DIR"

# Stop existing containers
docker compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

# Build and start
docker compose -f docker-compose.prod.yml up -d --build

# ─── Step 6: Verify deployment ───
log_info "Verifying deployment..."
sleep 10

# Check all containers are running
log_info "Container status:"
docker compose -f docker-compose.prod.yml ps

# Check backend health
log_info "Checking backend health..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
if [ "$HEALTH_CHECK" = "200" ]; then
    log_info "✅ Backend is healthy (HTTP 200)"
else
    log_error "❌ Backend health check failed (HTTP $HEALTH_CHECK)"
    log_info "Checking logs..."
    docker compose -f docker-compose.prod.yml logs --tail=50 app
fi

# Check Prometheus
PROM_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/healthy 2>/dev/null || echo "000")
if [ "$PROM_CHECK" = "200" ]; then
    log_info "✅ Prometheus is healthy"
else
    log_warn "⚠️  Prometheus health check failed (HTTP $PROM_CHECK)"
fi

# ─── Step 7: Summary ───
log_info "═══════════════════════════════════════════"
log_info "  Deployment completed!"
log_info ""
log_info "  Application: https://magazyn.kzelman.pl"
log_info "  Grafana:     http://localhost:3000"
log_info "  Prometheus:  http://localhost:9090"
log_info ""
log_info "  Run 'docker compose -f docker-compose.prod.yml logs -f' to watch logs"
log_info "═══════════════════════════════════════════"
