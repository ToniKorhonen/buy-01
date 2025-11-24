#!/bin/bash
set -e

echo "🚀 Jenkins Deployment Script"
echo "=============================="
echo ""

# Configuration
MAX_WAIT=300  # 5 minutes max wait for health checks
HEALTH_CHECK_INTERVAL=10

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to check service health
check_service_health() {
    local service_name=$1
    local port=$2
    local max_attempts=$((MAX_WAIT / HEALTH_CHECK_INTERVAL))
    local attempt=1

    echo -e "${YELLOW}⏳ Waiting for ${service_name} on port ${port}...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if nc -z localhost $port 2>/dev/null; then
            echo -e "${GREEN}✅ ${service_name} is healthy${NC}"
            return 0
        fi

        if [ $attempt -eq $max_attempts ]; then
            echo -e "${RED}❌ ${service_name} failed to start${NC}"
            return 1
        fi

        echo "   Attempt $attempt/$max_attempts - waiting ${HEALTH_CHECK_INTERVAL}s..."
        sleep $HEALTH_CHECK_INTERVAL
        attempt=$((attempt + 1))
    done

    return 1
}

# Function to rollback on failure
rollback() {
    echo -e "${RED}🔄 Deployment failed! Rolling back...${NC}"
    docker compose down
    echo -e "${RED}❌ Rollback complete. Please check the logs.${NC}"
    exit 1
}

# Trap errors and rollback
trap 'rollback' ERR

echo "📋 Step 1: Stopping existing containers..."
docker compose down || true

echo ""
echo "🐳 Step 2: Starting services with Docker Compose..."
docker compose up -d --build

echo ""
echo "🏥 Step 3: Running health checks..."
echo ""

# Wait a bit for containers to initialize
sleep 5

# Check MongoDB first (foundational service)
check_service_health "MongoDB" 27017 || rollback

# Check backend services
check_service_health "User Service" 8081 || rollback
check_service_health "Product Service" 8082 || rollback
check_service_health "Media Service" 8083 || rollback

# Check API Gateway
check_service_health "API Gateway" 8080 || rollback

# Check Frontend
check_service_health "Frontend (HTTP)" 4200 || rollback
check_service_health "Frontend (HTTPS)" 4443 || rollback

echo ""
echo -e "${GREEN}=============================="
echo "✅ Deployment Successful!"
echo -e "==============================${NC}"
echo ""
echo "🌐 Application URLs:"
echo "   - Frontend (HTTPS): https://localhost:4443"
echo "   - Frontend (HTTP):  http://localhost:4200"
echo "   - API Gateway:      http://localhost:8080"
echo ""
echo "📊 Service Status:"
docker compose ps
echo ""
echo -e "${GREEN}🎉 All services are running!${NC}"

