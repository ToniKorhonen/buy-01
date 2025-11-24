# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci && npm cache clean --force
COPY . .
RUN npm run build -- --configuration=production

# Runtime stage - minimal
FROM node:20-alpine
RUN apk add --no-cache wget curl && \
    rm -rf /var/cache/apk/*
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force
COPY --from=build /app/dist/Frontend ./dist/Frontend
COPY server.mjs ./
COPY certs ./certs
EXPOSE 4200 4443
ENV NODE_ENV=production
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider https://localhost:4443 --no-check-certificate || exit 1
CMD ["node", "server.mjs"]
