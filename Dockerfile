# Multi-stage Docker build for voice-backend
# Stage 1: Build the application
FROM openjdk:21-jdk-slim AS builder

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files first (for better layer caching)
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r voiceapp && useradd -r -g voiceapp voiceapp

# Set working directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/target/voice-detection-*.jar app.jar

# Create directory for vector store and set permissions
RUN mkdir -p /app/vector-store && chown -R voiceapp:voiceapp /app

# Switch to non-root user
USER voiceapp

# Expose port (Azure will provide actual port via PORT env var)
EXPOSE 8080

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check (use PORT env var if available)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/voice-interaction/health || exit 1

# Start the application with debug output
ENTRYPOINT ["sh", "-c", "echo 'Starting with profile:' $SPRING_PROFILES_ACTIVE && echo 'Java opts:' $JAVA_OPTS && echo 'Port:' $PORT && java $JAVA_OPTS -jar app.jar"] 