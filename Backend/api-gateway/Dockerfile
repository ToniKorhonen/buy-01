# Multi-stage build for minimal image size
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent POM and shared-commons for dependency resolution
COPY pom.xml .
COPY shared-commons ./shared-commons
COPY api-gateway ./api-gateway

# Build shared-commons first, then the specific service
RUN mvn clean install -DskipTests -Dmaven.javadoc.skip=true -f shared-commons/pom.xml && \
    mvn clean package -DskipTests -Dmaven.javadoc.skip=true -f api-gateway/pom.xml

# Minimal runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user, install packages, setup directories in one layer
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    apk add --no-cache curl wget && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app && \
    chown appuser:appgroup /app

WORKDIR /app

# Copy JAR with root ownership and set read-only permissions
COPY --chown=root:root --from=build /app/api-gateway/target/*.jar app.jar
RUN chmod 444 app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080
# Optimized JVM settings for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use exec form for ENTRYPOINT
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
