#!/bin/bash

# Load environment variables from .env file
if [[ -f .env ]]; then
    echo "Loading environment variables from .env file..."
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
    echo "✓ Environment variables loaded"
else
    echo "ERROR: .env file not found!" >&2
    echo "Please create a .env file based on .env.example" >&2
    echo "Generate a secure JWT secret using: openssl rand -base64 64" >&2
    exit 1
fi

# Validate required environment variables
if [[ -z "$JWT_SECRET" ]]; then
    echo "ERROR: JWT_SECRET environment variable is not set!" >&2
    echo "Please set JWT_SECRET in your .env file" >&2
    exit 1
fi

echo "Starting services with externalized configuration..."
echo "JWT_SECRET: [REDACTED - ${#JWT_SECRET} characters]"
echo "JWT_EXPIRATION: ${JWT_EXPIRATION}"
echo ""

# You can now start your services
# Example:
# cd Backend/user-service && mvn spring-boot:run &
# cd Backend/product-service && mvn spring-boot:run &
# cd Backend/media-service && mvn spring-boot:run &
# cd Backend/api-gateway && mvn spring-boot:run &

echo "Environment is ready. Start your services using:"
echo "  cd Backend/user-service && mvn spring-boot:run"
echo "  cd Backend/product-service && mvn spring-boot:run"
echo "  cd Backend/media-service && mvn spring-boot:run"
echo "  cd Backend/api-gateway && mvn spring-boot:run"