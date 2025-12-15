#!/bin/bash
set -e

echo "🐳 Starting Buy-01 Application with Docker Compose..."
echo ""

# Note: Local MongoDB (port 27017) and Docker MongoDB (port 27018) can run simultaneously
# Build and start all services
docker compose up --build -d
echo ""
echo "✅ Services are starting up..."
echo ""
echo "⏳ Waiting for services to be healthy (this may take 1-2 minutes)..."
sleep 10
# Check status
docker compose ps
echo ""
echo "🌐 Application URLs:"
echo "   Frontend (HTTPS): https://localhost:4443"
echo "   Frontend (HTTP):  http://localhost:4200"
echo "   API Gateway:      http://localhost:8080"
echo "   MongoDB:          mongodb://localhost:27018"
echo ""
echo "📊 Useful commands:"
echo "   Check status:  docker compose ps"
echo "   View logs:     docker compose logs -f [service-name]"
echo "   Stop:          ./docker-stop.sh"
echo "   Restart:       docker compose restart [service-name]"
echo ""
