#!/bin/bash

echo "🔄 Rebuilding Voice Backend Container..."

# Stop and remove existing container
echo "⏹️ Stopping existing container..."
docker-compose down

# Rebuild the image
echo "🏗️ Building new image..."
docker-compose build --no-cache

# Start the new container
echo "🚀 Starting new container..."
docker-compose up -d

# Show container status
echo "📊 Container status:"
docker-compose ps

# Show logs for a few seconds
echo "📝 Container logs:"
docker-compose logs --tail=20 voice-backend

echo "✅ Rebuild complete! Container running on http://localhost:8081" 