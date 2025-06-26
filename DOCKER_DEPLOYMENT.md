# Docker Deployment Guide - Voice Backend

## Quick Start Commands

```bash
# 1. Build the Docker image
docker build -t voice-backend .

# 2. Run locally with environment variables
docker run -d \
  --name voice-backend \
  -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  -e DEEPGRAM_API_KEY=your-key \
  -e ELEVENLABS_API_KEY=your-key \
  voice-backend

# 3. Or use docker-compose for easier management
docker-compose up -d
```

## Environment Setup

### Create .env file (optional for docker-compose):
```bash
# .env file
OPENAI_API_KEY=sk-proj-your-actual-key
DEEPGRAM_API_KEY=your-deepgram-key
ELEVENLABS_API_KEY=sk_your-elevenlabs-key
```

## Local Development

### Development Mode:
```bash
# Start development container
docker-compose up voice-backend

# View logs
docker-compose logs -f voice-backend

# Stop
docker-compose down
```

### Production Testing:
```bash
# Test production configuration locally
docker-compose --profile prod-test up voice-backend-prod

# Access at http://localhost:8081
```

## Azure Container Deployment

### Option 1: Azure Container Instances (Recommended)

1. **Build and tag image**:
   ```bash
   docker build -t voice-backend .
   docker tag voice-backend your-registry.azurecr.io/voice-backend:latest
   ```

2. **Push to Azure Container Registry**:
   ```bash
   # Login to Azure
   az login
   az acr login --name your-registry
   
   # Push image
   docker push your-registry.azurecr.io/voice-backend:latest
   ```

3. **Deploy to Azure Container Instances**:
   ```bash
   az container create \
     --resource-group your-resource-group \
     --name voice-backend \
     --image your-registry.azurecr.io/voice-backend:latest \
     --dns-name-label voice-backend-unique \
     --ports 8080 \
     --environment-variables \
       SPRING_PROFILES_ACTIVE=prod \
       OPENAI_API_KEY=your-key \
       DEEPGRAM_API_KEY=your-key \
       ELEVENLABS_API_KEY=your-key \
     --cpu 1 \
     --memory 2
   ```

### Option 2: Azure App Service (Container)

1. **Create App Service Plan**:
   ```bash
   az appservice plan create \
     --name voice-backend-plan \
     --resource-group your-resource-group \
     --sku B1 \
     --is-linux
   ```

2. **Create Web App**:
   ```bash
   az webapp create \
     --resource-group your-resource-group \
     --plan voice-backend-plan \
     --name voice-backend-app \
     --deployment-container-image-name your-registry.azurecr.io/voice-backend:latest
   ```

3. **Configure environment variables**:
   ```bash
   az webapp config appsettings set \
     --resource-group your-resource-group \
     --name voice-backend-app \
     --settings \
       SPRING_PROFILES_ACTIVE=prod \
       OPENAI_API_KEY=your-key \
       DEEPGRAM_API_KEY=your-key \
       ELEVENLABS_API_KEY=your-key \
       WEBSITES_ENABLE_APP_SERVICE_STORAGE=false \
       WEBSITES_PORT=8080
   ```

## Docker Image Features

### Multi-stage Build:
- **Stage 1**: Builds JAR with Maven
- **Stage 2**: Runtime image with OpenJDK 17
- **Result**: Smaller final image (~200MB vs ~800MB)

### Security:
- Runs as non-root user (`voiceapp`)
- Minimal base image (slim)
- No sensitive data in image layers

### Optimizations:
- Layer caching for faster builds
- G1 garbage collector for better performance
- Health checks for container orchestration
- Proper signal handling

## Testing Your Container

### Health Check:
```bash
# Check if container is healthy
docker ps
curl http://localhost:8080/api/health
```

### Navigation Test:
```bash
# Test navigation endpoint
curl -X POST http://localhost:8080/api/voice/test-navigate \
  -H "Content-Type: application/json" \
  -d '{"command": "navigate", "target": "/profile"}'
```

### WebSocket Test:
```bash
# Test WebSocket connection (use a WebSocket client)
wscat -c ws://localhost:8080/ws/ui-control
```

## Troubleshooting

### Container Won't Start:
```bash
# Check logs
docker logs voice-backend

# Common issues:
# - Missing environment variables
# - Port conflicts
# - Insufficient memory
```

### Health Check Failing:
```bash
# Check internal health endpoint
docker exec voice-backend curl http://localhost:8080/api/health

# Check container resources
docker stats voice-backend
```

### WebSocket Issues:
```bash
# Check if port is exposed
docker port voice-backend

# Verify WebSocket configuration in logs
docker logs voice-backend | grep -i websocket
```

## Production Checklist

- [ ] **Environment Variables**: All API keys set securely
- [ ] **Resource Limits**: CPU and memory limits configured
- [ ] **Health Checks**: Container health monitoring enabled
- [ ] **Logging**: Structured logging configured
- [ ] **Security**: Container runs as non-root user
- [ ] **Networking**: Proper firewall and networking rules
- [ ] **SSL**: HTTPS termination configured (Azure handles this)
- [ ] **Monitoring**: Application insights or monitoring enabled

## Cost Optimization

### Azure Container Instances:
- **Basic**: 1 vCPU, 2GB RAM ≈ $30-40/month
- **Auto-scaling**: Scale to zero when not in use
- **Pay-per-second**: Only pay when running

### Azure App Service (Container):
- **B1 Basic**: $13/month (always on)
- **S1 Standard**: $56/month (auto-scaling, staging slots)

## Scaling Options

### Horizontal Scaling:
```bash
# Scale container instances
az container create --name voice-backend-2 [same config]

# Load balancer in front of multiple instances
```

### Vertical Scaling:
```bash
# Increase resources
az container create \
  --cpu 2 \
  --memory 4 \
  [other options]
```

## Backup Strategy

### Vector Store Data:
- Mount persistent volumes for `/app/vector-store`
- Regular backups of vector data
- Consider Azure File Share for persistence

### Configuration:
- Store environment variables in Azure Key Vault
- Use managed identities for secure access