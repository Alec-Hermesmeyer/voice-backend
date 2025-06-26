# Security Configuration

## Environment Variables

This project requires API keys that **MUST NOT** be committed to git. All sensitive configuration is handled via environment variables.

### Required Environment Variables:

```bash
OPENAI_API_KEY=sk-proj-your-actual-key
DEEPGRAM_API_KEY=your-deepgram-key  
ELEVENLABS_API_KEY=sk_your-elevenlabs-key
PICOVOICE_ACCESS_KEY=your-picovoice-key
```

### Local Development Setup:

1. **Copy the example file**:
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual keys**:
   ```bash
   # Edit with your preferred editor
   nano .env
   ```

3. **Load environment variables**:
   ```bash
   # Option 1: Export manually
   export OPENAI_API_KEY=your-key
   export DEEPGRAM_API_KEY=your-key
   
   # Option 2: Use direnv (if installed)
   direnv allow
   
   # Option 3: Source the file
   set -a && source .env && set +a
   ```

### Docker Development:

```bash
# Using .env file with docker-compose
docker-compose up

# Or pass environment variables directly
docker run -d \
  -e OPENAI_API_KEY=your-key \
  -e DEEPGRAM_API_KEY=your-key \
  voice-backend
```

### Production Deployment:

**Azure Container Instances:**
```bash
az container create \
  --environment-variables \
    OPENAI_API_KEY=your-key \
    DEEPGRAM_API_KEY=your-key \
  # ... other options
```

**Azure App Service:**
```bash
az webapp config appsettings set \
  --settings \
    OPENAI_API_KEY=your-key \
    DEEPGRAM_API_KEY=your-key
```

## Security Best Practices

### ✅ What we do:
- All API keys use environment variables
- No hardcoded secrets in code
- `.env` files are gitignored
- Fallback values are clearly marked as missing
- Docker containers run as non-root user

### ❌ What to avoid:
- Never commit actual API keys to git
- Don't share `.env` files
- Don't log API keys
- Don't include keys in error messages
- Don't store keys in application.properties with real values

## Emergency: If API Keys Were Committed

If API keys were accidentally committed:

1. **Revoke the compromised keys immediately**:
   - OpenAI: https://platform.openai.com/api-keys
   - Deepgram: https://console.deepgram.com/
   - ElevenLabs: https://elevenlabs.io/

2. **Generate new API keys**

3. **Remove from git history**:
   ```bash
   # This rewrites history - coordinate with team!
   git filter-branch --force --index-filter \
     'git rm --cached --ignore-unmatch src/main/resources/application*.properties' \
     --prune-empty --tag-name-filter cat -- --all
   ```

4. **Update environment variables everywhere** (local, CI/CD, production)

## File Status

- ✅ `application.properties` - Safe to commit (no real keys)
- ✅ `application-dev.properties` - Safe to commit (no real keys)  
- ✅ `application-prod.properties` - Safe to commit (no real keys)
- ❌ `.env` - Never commit (contains real keys)
- ✅ `.env.example` - Safe to commit (examples only)