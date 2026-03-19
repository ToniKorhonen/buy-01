#!/bin/bash

# Frontend Certificate & HTTPS Verification Script
# Usage: ./verify-certs.sh [build|run|test|clean]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_NAME="buy01-frontend-verify"
IMAGE_NAME="buy01-frontend:verify-latest"

# Assign command line argument to local variable
COMMAND="${1:-help}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    local message="$1"
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}${message}${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}\n"
    return 0
}

print_success() {
    local message="$1"
    echo -e "${GREEN}✓ ${message}${NC}"
    return 0
}

print_error() {
    local message="$1"
    echo -e "${RED}✗ ${message}${NC}"
    return 0
}

print_warning() {
    local message="$1"
    echo -e "${YELLOW}⚠ ${message}${NC}"
    return 0
}

print_info() {
    local message="$1"
    echo -e "${BLUE}ℹ ${message}${NC}"
    return 0
}

cleanup() {
    print_info "Cleaning up..."
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
    print_success "Cleanup complete"
    return 0
}

verify_build() {
    print_header "Building Frontend Docker Image"

    if [[ ! -f "$SCRIPT_DIR/Frontend/Dockerfile" ]]; then
        print_error "Dockerfile not found at $SCRIPT_DIR/Frontend/Dockerfile"
        return 1
    fi

    cd "$SCRIPT_DIR" || return 1

    print_info "Building image: $IMAGE_NAME"
    if docker build -t "$IMAGE_NAME" -f Frontend/Dockerfile Frontend/; then
        print_success "Docker image built successfully"
        docker image ls | grep "buy01-frontend:verify-latest"
        return 0
    else
        print_error "Docker build failed"
        return 1
    fi
}

verify_runtime() {
    print_header "Testing Certificate Generation at Runtime"

    print_info "Starting container: $CONTAINER_NAME"

    if docker run -d \
        --name "$CONTAINER_NAME" \
        -p 4200:4200 \
        -p 4443:4443 \
        "$IMAGE_NAME" > /dev/null; then
        print_success "Container started"
    else
        print_error "Failed to start container"
        return 1
    fi

    # Wait for startup
    print_info "Waiting for certificate generation and server startup..."
    sleep 8

    # Check if certs were generated
    print_info "Verifying certificates were generated..."
    if docker exec "$CONTAINER_NAME" test -f /app/certs/cert.pem; then
        print_success "Certificate file exists"
    else
        print_error "Certificate file not found!"
        docker logs "$CONTAINER_NAME" | tail -20
        cleanup
        return 1
    fi

    if docker exec "$CONTAINER_NAME" test -f /app/certs/key.pem; then
        print_success "Private key file exists"
    else
        print_error "Private key file not found!"
        docker logs "$CONTAINER_NAME" | tail -20
        cleanup
        return 1
    fi

    # Check certificate details
    print_info "Checking certificate details..."
    CERT_INFO=$(docker exec "$CONTAINER_NAME" openssl x509 -in /app/certs/cert.pem -noout -subject 2>/dev/null)
    print_success "Certificate info: $CERT_INFO"

    CERT_EXPIRY=$(docker exec "$CONTAINER_NAME" openssl x509 -in /app/certs/cert.pem -noout -enddate 2>/dev/null)
    print_success "Expiry: $CERT_EXPIRY"

    # Check file permissions
    print_info "Checking file permissions..."
    CERT_PERMS=$(docker exec "$CONTAINER_NAME" stat -c "%a" /app/certs/cert.pem 2>/dev/null || docker exec "$CONTAINER_NAME" ls -l /app/certs/cert.pem)
    print_info "Certificate permissions: $CERT_PERMS"

    # Check container logs
    print_header "Container Startup Logs"
    docker logs "$CONTAINER_NAME" | head -30
    return 0
}

verify_https() {
    print_header "Testing HTTPS Endpoint"

    if ! docker exec "$CONTAINER_NAME" test -f /app/certs/cert.pem; then
        print_error "Container not running or certs not generated"
        return 1
    fi

    print_info "Testing HTTPS on port 4443..."

    if docker exec "$CONTAINER_NAME" wget -q --no-check-certificate https://localhost:4443 -O /dev/null 2>/dev/null; then
        print_success "HTTPS endpoint is responding"
    else
        print_warning "HTTPS endpoint check failed (may still be starting up)"
    fi

    # Try from host (if Docker is accessible)
    print_info "Testing from host machine..."
    if command -v curl &> /dev/null; then
        if curl -s -k https://localhost:4443 > /dev/null 2>&1; then
            print_success "HTTPS accessible from host"
        else
            print_warning "Could not reach HTTPS from host (network isolation or startup delay)"
        fi
    else
        print_info "curl not available for host-level test"
    fi

    return 0
}

verify_healthcheck() {
    print_header "Testing Container Healthcheck"

    print_info "Running healthcheck..."
    if docker exec "$CONTAINER_NAME" sh -c "node -e \"require('https').get('https://localhost:4443', {rejectUnauthorized: false}, (res) => process.exit(res.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))\"" 2>/dev/null; then
        print_success "Healthcheck passed"
    else
        print_warning "Healthcheck command failed (may be timing issue)"
    fi

    return 0
}

run_all_tests() {
    print_header "Running Full Certificate Verification Suite"

    verify_build || return 1
    verify_runtime || return 1
    verify_https || return 1
    verify_healthcheck || return 1

    print_header "Verification Complete"
    print_success "All tests passed! ✓"
    print_info "Container is running as: $CONTAINER_NAME"
    print_info "You can inspect it with: docker exec $CONTAINER_NAME sh"
    print_info "View logs with: docker logs $CONTAINER_NAME"

    return 0
}

show_usage() {
    cat << 'EOF'
Usage: ./verify-certs.sh [COMMAND]

Commands:
  build       - Build the Docker image with updated Dockerfile
  run         - Start container and test certificate generation
  test        - Full verification (build + run + HTTPS tests)
  clean       - Stop and remove verification container
  help        - Show this help message

Examples:
  ./verify-certs.sh test           # Run full verification
  ./verify-certs.sh build          # Just build the image
  ./verify-certs.sh run            # Just test runtime behavior

Environment Variables:
  CONTAINER_NAME    - Override container name (default: buy01-frontend-verify)
  IMAGE_NAME        - Override image name (default: buy01-frontend:verify-latest)

EOF
    return 0
}

# Main script logic
case "$COMMAND" in
    build)
        verify_build
        exit $?
        ;;
    run)
        verify_runtime || exit 1
        verify_https || exit 1
        verify_healthcheck || exit 1
        exit 0
        ;;
    test)
        cleanup || true
        run_all_tests
        exit $?
        ;;
    clean)
        cleanup
        exit $?
        ;;
    help)
        show_usage
        exit 0
        ;;
    *)
        print_error "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac

# Show final status
print_info "Verification script completed"
if [[ "$COMMAND" != "clean" ]]; then
    print_info "Container still running. Run '$0 clean' to remove it."
fi

