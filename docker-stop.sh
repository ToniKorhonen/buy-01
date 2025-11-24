#!/bin/bash
set -e
echo "🛑 Stopping Buy-01 Application..."

# Try stopping with buy01 project name (Jenkins deployment)
if [ "$(docker ps -q -f name=buy01-)" ]; then
    echo "📦 Found containers with buy01 project name (Jenkins deployment)"
    COMPOSE_PROJECT_NAME=buy01 docker compose down
else
    # Fallback to default project name
    docker compose down
fi

echo "✅ All services stopped"
echo ""
echo "💡 To remove volumes (DELETE ALL DATA):"
echo "   COMPOSE_PROJECT_NAME=buy01 docker compose down -v"
