#!/bin/bash

# Voice Backend Docker Test Script

echo "🐳 Voice Backend Docker Test"
echo "============================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

echo "✅ Docker is running"

# Build the image
echo "🏗️  Building Docker image..."
if docker build -t voice-backend .; then
    echo "✅ Docker build successful"
else
    echo "❌ Docker build failed"
    exit 1
fi

# Check image size
echo "📊 Image size:"
docker images voice-backend --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# Test with environment variables (you'll need to set these)
echo ""
echo "🧪 Testing container startup..."
echo "Note: Set your API keys as environment variables first:"
echo "  export OPENAI_API_KEY=your-key"
echo "  export DEEPGRAM_API_KEY=your-key"
echo "  export ELEVENLABS_API_KEY=your-key"

# Start container in background
echo "🚀 Starting container..."
docker run -d \
  --name voice-backend-test \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e OPENAI_API_KEY="${OPENAI_API_KEY:-dummy-key}" \
  -e DEEPGRAM_API_KEY="${DEEPGRAM_API_KEY:-dummy-key}" \
  -e ELEVENLABS_API_KEY="${ELEVENLABS_API_KEY:-dummy-key}" \
  voice-backend

if [ $? -eq 0 ]; then
    echo "✅ Container started successfully"
    
    # Wait for startup
    echo "⏳ Waiting for application to start..."
    sleep 30
    
    # Test health endpoint
    echo "🏥 Testing health endpoint..."
    if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✅ Health check passed"
    else
        echo "❌ Health check failed"
        echo "📋 Container logs:"
        docker logs voice-backend-test
    fi
    
    # Test navigation endpoint
    echo "🧭 Testing navigation endpoint..."
    if curl -f -X POST http://localhost:8080/api/voice/test-navigate \
       -H "Content-Type: application/json" \
       -d '{"command": "navigate", "target": "/profile"}' > /dev/null 2>&1; then
        echo "✅ Navigation test passed"
    else
        echo "❌ Navigation test failed"
    fi
    
    echo ""
    echo "🎉 Docker container is running at http://localhost:8080"
    echo "📋 View logs: docker logs voice-backend-test"
    echo "🛑 Stop container: docker stop voice-backend-test"
    echo "🗑️  Remove container: docker rm voice-backend-test"
    
else
    echo "❌ Failed to start container"
    exit 1
fi