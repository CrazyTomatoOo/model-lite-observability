# Build JAR first: mvn clean package -DskipTests
# Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup -u 1000

WORKDIR /app

# Copy pre-built JAR from local filesystem
COPY target/*.jar app.jar

# Switch to non-root user
USER appuser

# Default JVM options
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT java ${JAVA_OPTS} -jar app.jar
