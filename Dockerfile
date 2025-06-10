FROM openjdk:21-jdk-slim

WORKDIR /app

# Install system dependencies including Maven
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Create directory for vector store
RUN mkdir -p /app/vector-store

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/voice-interaction/health || exit 1

# Run the application
CMD ["java", "-jar", "target/voice-detection-1.0-SNAPSHOT.jar"] 