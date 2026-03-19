# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --ignore-scripts && npm cache clean --force
# Only copy necessary source files, not entire context
COPY angular.json tsconfig*.json ./
COPY public ./public
COPY src ./src
RUN npm run build -- --configuration=production

# Runtime stage - minimal
FROM node:20-alpine

# Create non-root user and group, install runtime packages in one layer
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    apk add --no-cache curl wget && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app/certs

WORKDIR /app

# Copy files and immediately set read-only permissions
COPY --chown=root:root package*.json ./
COPY --chown=root:root --from=build /app/dist/Frontend ./dist/Frontend
COPY --chown=root:root server.mjs ./
COPY --chown=root:root cert-generator.mjs ./

# Install dependencies and harden file permissions
# Create certs directory with proper permissions (will be populated by cert-generator at runtime)
RUN npm ci --only=production --ignore-scripts && \
    npm cache clean --force && \
    chmod -R 555 dist/ && \
    chmod 444 server.mjs cert-generator.mjs package*.json package-lock.json && \
    mkdir -p /app/certs && \
    chmod 755 /app/certs && \
    find node_modules -type d -exec chmod 555 {} \; && \
    find node_modules -type f -exec chmod 444 {} \; && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

EXPOSE 4200 4443
ENV NODE_ENV=production
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD node -e "require('https').get('https://localhost:4443', {rejectUnauthorized: false}, (res) => process.exit(res.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))"
CMD ["sh", "-c", "node cert-generator.mjs && node server.mjs"]
