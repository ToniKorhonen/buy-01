#!/bin/bash
set -e
echo "🛑 Stopping Buy-01 Application..."
docker compose down
echo "✅ All services stopped"
echo ""
echo "💡 To remove volumes (DELETE ALL DATA): docker compose down -v"
