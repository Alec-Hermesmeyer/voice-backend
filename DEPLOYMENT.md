# Deployment Guide - Free Hosting Options

## Quick Deployment Options

### Option 1: Railway (Recommended for Testing)

**Why Railway?**
- Zero-config Spring Boot deployment
- 500 free hours/month
- Persistent file storage
- Automatic HTTPS
- Environment variable management

**Setup Steps:**

1. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Prepare for Railway deployment"
   git push origin main
   ```

2. **Deploy on Railway:**
   - Go to [railway.app](https://railway.app)
   - Connect your GitHub account
   - Select your `voice-backend` repository
   - Railway auto-detects Spring Boot

3. **Set Environment Variables:**
   ```
   OPENAI_API_KEY=your_openai_key
   DEEPGRAM_API_KEY=your_deepgram_key (optional)
   PICOVOICE_ACCESS_KEY=your_picovoice_key (optional)
   SPRING_PROFILES_ACTIVE=production
   ```

4. **Your API will be live at:**
   ```
   https://your-app-name.up.railway.app
   ```

### Option 2: Fly.io (More Robust)

**Why Fly.io?**
- Always-on free tier
- 3 free VMs
- Better performance
- Persistent volumes

**Setup Steps:**

1. **Install Fly CLI:**
   ```bash
   # macOS
   brew install flyctl
   
   # Login
   flyctl auth login
   ```

2. **Initialize and Deploy:**
   ```bash
   # Initialize (will use existing fly.toml)
   flyctl launch --no-deploy
   
   # Set secrets
   flyctl secrets set OPENAI_API_KEY=your_openai_key
   flyctl secrets set DEEPGRAM_API_KEY=your_deepgram_key
   flyctl secrets set PICOVOICE_ACCESS_KEY=your_picovoice_key
   
   # Create volume for data persistence
   flyctl volumes create voice_data --region iad --size 1
   
   # Deploy
   flyctl deploy
   ```

3. **Your API will be live at:**
   ```
   https://voice-backend.fly.dev
   ```

### Option 3: Render (Simple Alternative)

**Setup Steps:**

1. **Connect GitHub to Render:**
   - Go to [render.com](https://render.com)
   - Connect your GitHub repository

2. **Create Web Service:**
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/voice-backend-0.0.1-SNAPSHOT.jar`
   - Environment: `Java`

3. **Set Environment Variables in Render dashboard**

## Testing Your Deployment

### Health Check
```bash
curl https://your-domain/api/voice-interaction/health
```

### Test Voice Session
```bash
# Start session
curl -X POST https://your-domain/api/voice-interaction/sessions/start \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "test-client",
    "welcomeMessage": "Hello! I am ready to help you navigate.",
    "voice": "alloy"
  }'

# Test input
curl -X POST https://your-domain/api/voice-interaction/sessions/{sessionId}/input \
  -H "Content-Type: application/json" \
  -d '{
    "audioData": "base64_audio_data",
    "text": "Hello, can you help me navigate to the dashboard?"
  }'
```

## Frontend Integration

Update your frontend to use the live endpoints:

```javascript
// Replace localhost with your deployed URL
const API_BASE_URL = 'https://your-app-name.up.railway.app';

// Voice session management
const startVoiceSession = async (clientId) => {
  const response = await fetch(`${API_BASE_URL}/api/voice-interaction/sessions/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      clientId,
      welcomeMessage: "Ready to help you navigate!",
      voice: "alloy"
    })
  });
  return response.json();
};

// WebSocket connection for real-time UI updates
const connectWebSocket = (sessionId) => {
  const ws = new WebSocket(`wss://your-domain/ws/ui-control`);
  ws.onmessage = (event) => {
    const command = JSON.parse(event.data);
    executeUICommand(command);
  };
  return ws;
};
```

## Production Considerations

### Scaling to Paid Plans
- **Railway**: $5/month for 8GB RAM, unlimited hours
- **Fly.io**: $1.94/month per additional VM
- **Render**: $7/month for always-on service

### Security
- Enable CORS for your frontend domain
- Use HTTPS (automatic on all platforms)
- Secure your API keys in environment variables

### Monitoring
- Railway: Built-in metrics dashboard
- Fly.io: Built-in monitoring and logs
- Render: Application logs and metrics

## Troubleshooting

### Common Issues:
1. **Cold starts**: Free tiers may have startup delays
2. **Memory limits**: Optimize JVM settings if needed
3. **File storage**: Ensure vector-store directory persists

### Debug Commands:
```bash
# Railway logs
railway logs

# Fly.io logs
flyctl logs

# Check app status
flyctl status
```

## Cost Optimization

**Free Tier Limits:**
- Railway: 500 hours/month (≈20 days of always-on)
- Fly.io: Always-on but limited resources
- Render: Sleeps after 15min inactivity

**Tips:**
- Use Railway for development/testing
- Upgrade to Fly.io paid for production
- Implement proper session management to handle cold starts 