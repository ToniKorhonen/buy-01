FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true

FROM eclipse-temurin:17-jre-alpine

# Create non-root user, install packages, setup directories with proper permissions in one layer
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    apk add --no-cache curl wget && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app/uploads && \
    chown appuser:appgroup /app /app/uploads && \
    chmod 755 /app/uploads

WORKDIR /app

# Copy JAR with root ownership and set read-only permissions
COPY --chown=root:root target/*.jar app.jar
RUN chmod 444 app.jar

# Switch to non-root user
USER appuser

EXPOSE 8083
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Use exec form for ENTRYPOINT
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
