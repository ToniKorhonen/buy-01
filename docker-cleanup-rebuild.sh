#!/bin/bash

set -e

echo "🧹 Cleaning up Docker environment..."

# Stop and remove containers
docker compose down -v 2>/dev/null || true

# Remove containers
docker ps -a --filter "name=buy01" --format "{{.ID}}" | xargs -r docker rm -f 2>/dev/null || true

# Remove images
docker images | grep -E "buy-01|buy01" | awk '{print $3}' | sort -u | xargs -r docker rmi -f 2>/dev/null || true

# Prune
docker container prune -f
docker network prune -f

echo "✅ Cleanup complete!"
echo ""
echo "🏗️  Building and starting services..."

# Rebuild and start
docker compose up -d --build

echo ""
echo "⏳ Waiting for services to start..."
sleep 10

echo ""
echo "📊 Service Status:"
docker compose ps

echo ""
echo "✅ Done! Check logs with: docker compose logs -f"

