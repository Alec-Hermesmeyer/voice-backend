 # Azure Deployment Guide

## Prerequisites

1. **Azure Account** with App Service capability
2. **Updated API Keys** - Get fresh keys from:
   - OpenAI: https://platform.openai.com/api-keys  
   - Deepgram: https://console.deepgram.com/
   - ElevenLabs: https://elevenlabs.io/

## Environment Variables Required

Set these in Azure App Service → Configuration → Application Settings:

```bash
OPENAI_API_KEY=sk-proj-your-actual-key
DEEPGRAM_API_KEY=your-deepgram-key  
ELEVENLABS_API_KEY=sk_your-elevenlabs-key
SPRING_PROFILES_ACTIVE=prod
```

## Azure App Service Configuration

### 1. Basic Settings
- **Runtime**: Java 17
- **Operating System**: Linux (recommended)
- **Region**: Choose closest to your users
- **Pricing Tier**: B1 Basic or higher (for WebSocket support)

### 2. Application Settings
```bash
WEBSITE_JAVA_VERSION=17
WEBSITE_JAVA_CONTAINER=TOMCAT
WEBSITE_JAVA_CONTAINER_VERSION=9.0
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

### 3. WebSocket Support
- Enable **WebSockets** in Configuration → General settings
- Enable **Always On** to prevent cold starts

## Deployment Methods

### Option 1: JAR Upload (Recommended)

1. **Build the JAR**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Upload via Azure Portal**:
   - Go to App Service → Deployment Center
   - Choose "Local Git" or "ZIP Deploy"
   - Upload `target/voice-detection-1.0-SNAPSHOT.jar`

### Option 2: Git Deployment

1. **Connect Azure to your Git repo**:
   - App Service → Deployment Center → GitHub/Azure DevOps
   - Select repository and branch

2. **Azure will auto-build** using Maven

## Frontend Connection

Update your frontend WebSocket connections:

```javascript
// Replace localhost with your Azure URL
const wsUrl = 'wss://your-app.azurewebsites.net/ws/ui-control'

// SockJS connection (fallback)
const sock = new SockJS('https://your-app.azurewebsites.net/ws/ui-control')
```

## Testing Deployment

1. **Health Check**: 
   ```
   GET https://your-app.azurewebsites.net/api/health
   ```

2. **Navigation Test**:
   ```
   POST https://your-app.azurewebsites.net/api/voice/test-navigate
   {
     "command": "navigate",
     "target": "/profile"
   }
   ```

3. **WebSocket Test**: Connect to `/ws/ui-control` endpoint

## Production Checklist

- [ ] API keys set in Azure environment variables
- [ ] WebSockets enabled in Azure App Service
- [ ] Always On enabled to prevent cold starts
- [ ] CORS origins updated for your frontend domain
- [ ] SSL certificate configured (automatic with .azurewebsites.net)
- [ ] Frontend updated to use Azure WebSocket URLs

## Troubleshooting

### WebSocket Issues
- Ensure WebSockets are enabled in Azure
- Check CORS origins match your frontend domain
- Try SockJS fallback if native WebSockets fail

### Cold Start Issues  
- Enable "Always On" in Azure App Service
- Consider warming up endpoints with scheduled pings

### Memory Issues
- Monitor memory usage in Azure metrics
- Adjust `JAVA_OPTS` if needed
- Consider upgrading to higher tier

### API Key Issues
- Verify environment variables are set correctly
- Check API key validity and quotas
- Review application logs for authentication errors

## Monitoring

- **Azure Application Insights**: Enable for detailed monitoring
- **Logs**: View in Azure Portal → Log Stream
- **Metrics**: Monitor CPU, memory, and request metrics

## Cost Optimization

- **Basic B1**: ~$13/month (1 core, 1.75GB RAM)
- **Standard S1**: ~$56/month (1 core, 1.75GB RAM, more features)
- **Premium P1v2**: ~$73/month (1 core, 3.5GB RAM, better performance)

Start with B1 Basic and scale up based on usage.