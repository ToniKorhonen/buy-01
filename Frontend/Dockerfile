# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci && npm cache clean --force
COPY . .
RUN npm run build -- --configuration=production

# Runtime stage - minimal
FROM node:20-alpine
RUN apk add --no-cache wget curl openssl && \
    rm -rf /var/cache/apk/*
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force
COPY --from=build /app/dist/Frontend ./dist/Frontend
COPY server.mjs ./

# Copy certs from build stage if available, otherwise generate self-signed certificates
RUN mkdir -p certs
COPY --from=build /app/certs ./certs 2>/dev/null || true
RUN if [ ! -f certs/cert.pem ] || [ ! -f certs/key.pem ]; then \
        echo "Generating self-signed SSL certificates..."; \
        openssl req -x509 -newkey rsa:4096 -nodes \
            -keyout certs/key.pem \
            -out certs/cert.pem \
            -days 365 \
            -subj "/C=US/ST=State/L=City/O=Buy01/CN=localhost"; \
        chmod 644 certs/cert.pem certs/key.pem; \
    fi

EXPOSE 4200 4443
ENV NODE_ENV=production
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider https://localhost:4443 --no-check-certificate || exit 1
CMD ["node", "server.mjs"]
