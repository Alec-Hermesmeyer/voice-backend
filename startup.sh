#!/bin/bash

# Railway Startup Script for Voice Backend
echo "🚀 Starting Voice Backend Application..."

# Set production environment variables
export SPRING_PROFILES_ACTIVE=production
export JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# Create necessary directories
mkdir -p /app/vector-store
mkdir -p /app/user-data
mkdir -p /app/tts-cache

# Set permissions
chmod -R 755 /app/vector-store
chmod -R 755 /app/user-data
chmod -R 755 /app/tts-cache

echo "📁 Created application directories"

# Wait a moment to ensure file system is ready
sleep 2

# Start the application
echo "☕ Starting Java application..."
exec java $JAVA_OPTS -Dserver.port=$PORT -jar target/voice-detection-1.0-SNAPSHOT.jar 