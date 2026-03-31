FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent POM and shared-commons for dependency resolution
COPY pom.xml .
COPY shared-commons ./shared-commons
COPY order-service ./order-service

# Build shared-commons first, then the specific service
RUN mvn clean install -DskipTests -Dmaven.javadoc.skip=true -f shared-commons/pom.xml && \
    mvn clean package -DskipTests -Dmaven.javadoc.skip=true -f order-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    apk add --no-cache curl netcat-openbsd wget && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app && \
    chown appuser:appgroup /app
WORKDIR /app
COPY --chown=root:root --from=build /app/order-service/target/*.jar app.jar
RUN chmod 444 app.jar
USER appuser
EXPOSE 8084
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD nc -z localhost 8084 || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
