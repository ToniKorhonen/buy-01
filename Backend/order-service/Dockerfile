FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    apk add --no-cache curl netcat-openbsd wget && \
    rm -rf /var/cache/apk/* && \
    mkdir -p /app && \
    chown appuser:appgroup /app
WORKDIR /app
COPY --chown=root:root --from=build /app/target/*.jar app.jar
RUN chmod 444 app.jar
USER appuser
EXPOSE 8084
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD nc -z localhost 8084 || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
