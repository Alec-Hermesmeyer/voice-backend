# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot voice interaction backend that provides:
- Complete voice-controlled interface for web applications
- RAG (Retrieval-Augmented Generation) with client-specific knowledge bases
- Real-time WebSocket communication for UI control
- Speech-to-text, text-to-speech, and wake word detection
- Multi-client support with isolated knowledge bases

## Build and Development Commands

### Core Maven Commands
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Package for deployment
mvn clean package -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing
Currently no formal test suite is configured. Tests would be run with:
```bash
mvn test
```

### Deployment
The project supports multiple deployment targets:
- **Railway**: Zero-config deployment (recommended for testing)
- **Fly.io**: `flyctl deploy` (requires fly.toml configuration)
- **Render**: Uses render.yaml configuration
- **Docker**: `docker-compose up` for local containerized deployment

## Architecture Overview

### Core Services Architecture
- **VoiceInteractionService**: Main orchestrator for voice sessions and interactions
- **RAGService**: Manages client-specific knowledge bases with vector embeddings
- **CommandService**: Interprets voice commands and maps to UI actions
- **VectorStoreService**: Handles document embeddings and similarity search
- **SimpleWakeWordService**: Picovoice-based wake word detection
- **UIControlService**: Manages WebSocket communication for real-time UI updates

### WebSocket Endpoints
- `/ws/wake-word` - Wake word detection
- `/ws/audio` - Real-time audio transcription  
- `/ws/ui-control` - UI control commands and state updates

### Key API Patterns
- Voice sessions follow `/api/voice-interaction/sessions/{sessionId}/*` pattern
- RAG operations use `/api/rag/clients/{clientId}/*` pattern
- All voice processing integrates OpenAI Whisper, GPT-4, and TTS APIs

### Data Storage
- Vector embeddings stored in local JSON files under `vector-store/`
- TTS responses cached in `tts-cache/`
- User preferences and session data in `user-data/`

## Configuration

### Required Environment Variables
```bash
OPENAI_API_KEY=your_openai_key
DEEPGRAM_API_KEY=your_deepgram_key (optional)
PICOVOICE_ACCESS_KEY=your_picovoice_key (optional)
```

### Application Profiles
- `dev` - Development with verbose logging
- `production` - Production optimized settings
- `minimal` - Minimal feature set for testing

### Key Configuration Files
- `application.properties` - Base configuration
- `application-dev.properties` - Development overrides
- `application-production.properties` - Production settings

## Development Notes

### Voice Command Processing Flow
1. Wake word detection activates listening
2. Audio captured and sent to Whisper/Deepgram for STT
3. Transcript processed by CommandService for intent recognition
4. RAGService queries client knowledge base if needed
5. Response generated via GPT-4 with context
6. TTS converts response to audio
7. UI commands sent via WebSocket to frontend

### Adding New Voice Commands
1. Update `CommandService.COMMAND_WORDS` map
2. Add action mapping in `initializeCommandActions()`
3. Implement command handler method
4. Update UI context mappings in `UIControlService`

### RAG System Details
- Documents chunked into 500-character segments with 50-character overlap
- OpenAI text-embedding-ada-002 model for vector generation
- Cosine similarity threshold of 0.7 for relevant document retrieval
- Client isolation through separate vector stores per clientId

### WebSocket Integration
Frontend applications connect to `/ws/ui-control` and:
- Send UI state updates and context information
- Receive UI commands for navigation and interaction
- Handle voice feedback messages for TTS playback

### Health Monitoring
- Health check endpoint: `/api/voice-interaction/health`
- Application logs voice processing events and API failures
- WebSocket connection status monitoring included