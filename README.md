# Voice-Controlled Backend with RAG for Multi-Client Chat Applications

A sophisticated backend system that enables voice-controlled navigation and interaction with front-end applications, featuring Retrieval-Augmented Generation (RAG) capabilities for multiple clients. This system allows users to have natural conversations with their front-end applications while leveraging client-specific knowledge bases.

## 🎯 Overview

This backend provides:
- **Complete Voice-Only Interface**: Users can control applications entirely through voice
- **Voice Recognition**: Real-time speech-to-text using OpenAI Whisper and Deepgram
- **Natural Language Processing**: GPT-4 powered command interpretation
- **Smart Text-to-Speech**: Real-time audio responses using OpenAI TTS
- **RAG System**: Client-specific knowledge bases with semantic search
- **UI Control**: Voice commands mapped to frontend actions
- **Session Management**: Persistent voice sessions with conversation history
- **Wake Word Detection**: Hands-free activation using Picovoice
- **WebSocket Communication**: Real-time bidirectional communication
- **Multi-Client Support**: Isolated knowledge bases per client

## 🏗 Architecture

### Core Components

1. **Voice Processing Pipeline**
   - Wake word detection → Audio capture → Speech-to-text → Command processing

2. **RAG System**
   - Document chunking → Embedding generation → Vector storage → Semantic retrieval → Response generation

3. **UI Control System**
   - Command interpretation → Action mapping → WebSocket broadcasting → Frontend execution

## 📦 Dependencies

### Required Services
- **OpenAI API**: For embeddings, LLM processing, and Whisper STT
- **Deepgram API**: Alternative STT engine
- **Picovoice**: Wake word detection

### Java Dependencies (Maven)
- Spring Boot 3.2.0
- Spring WebSocket
- OkHttp 4.9.3
- Picovoice Java SDK 3.0.3
- JSON handling libraries

## ⚙️ Configuration

### Environment Variables
```bash
OPENAI_API_KEY=your_openai_api_key
DEEPGRAM_API_KEY=your_deepgram_api_key
```

### Application Properties
```properties
# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY}

# Deepgram Configuration  
deepgram.api.key=${DEEPGRAM_API_KEY}

# Server Configuration
server.port=8080

# CORS Configuration
cors.allowed.origins=http://localhost:3000,https://*.vercel.app
```

## 🚀 Getting Started

### 1. Clone and Setup
```bash
git clone <repository-url>
cd voice-backend
mvn clean install
```

### 2. Configure API Keys
Create a `.env` file or set environment variables:
```bash
export OPENAI_API_KEY="your_openai_key"
export DEEPGRAM_API_KEY="your_deepgram_key"
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## 📡 API Endpoints

### Voice Commands
```http
POST /api/voice/command
Content-Type: application/json

{
  "transcript": "navigate to dashboard",
  "currentContext": "main",
  "clientId": "client123"
}
```

### RAG Management

#### Initialize Client Knowledge Base
```http
POST /api/rag/clients/{clientId}/knowledge-base
Content-Type: application/json

{
  "documents": [
    {
      "id": "doc1",
      "content": "Document content here...",
      "source": "manual",
      "metadata": {"type": "guide", "version": "1.0"}
    }
  ]
}
```

#### Add Single Document
```http
POST /api/rag/clients/{clientId}/documents
Content-Type: application/json

{
  "id": "doc2",
  "content": "Additional document content...",
  "source": "api",
  "metadata": {"category": "faq"}
}
```

#### Upload Documents
```http
POST /api/rag/clients/{clientId}/upload
Content-Type: multipart/form-data

files: [file1.txt, file2.md]
source: "upload"
```

#### Process Voice Command with RAG
```http
POST /api/rag/clients/{clientId}/voice-command
Content-Type: application/json

{
  "voiceCommand": "How do I reset my password?",
  "currentContext": "settings"
}
```

#### Get Client Statistics
```http
GET /api/rag/clients/{clientId}/stats
```

#### Get All Clients Statistics
```http
GET /api/rag/stats
```

#### Delete Client Knowledge Base
```http
DELETE /api/rag/clients/{clientId}
```

### Voice Interaction Service

#### Start Voice Session
```http
POST /api/voice-interaction/sessions/start
Content-Type: application/json

{
  "sessionId": "voice-session-123",
  "clientId": "client123",
  "ttsEnabled": true,
  "voiceModel": "alloy",
  "welcomeMessage": "Welcome! How can I help you?"
}
```

#### Process Voice Input
```http
POST /api/voice-interaction/sessions/{sessionId}/input
Content-Type: application/json

{
  "transcript": "navigate to dashboard and show me the charts",
  "currentContext": "main-page"
}
```

#### Send TTS Message
```http
POST /api/voice-interaction/sessions/{sessionId}/speak
Content-Type: application/json

{
  "text": "Welcome to your dashboard. I can help you navigate."
}
```

#### Get Session Statistics
```http
GET /api/voice-interaction/sessions/{sessionId}/stats
```

#### End Voice Session
```http
POST /api/voice-interaction/sessions/{sessionId}/end
```

### UI Control
```http
GET /api/voice/actions/{context}
POST /api/voice/context
POST /api/voice/test-command
```

## 🔌 WebSocket Connections

### Available WebSocket Endpoints
- `ws://localhost:8080/ws/wake-word` - Wake word detection
- `ws://localhost:8080/ws/audio` - Real-time audio transcription
- `ws://localhost:8080/ws/ui-control` - UI control commands

### UI Control WebSocket Usage
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/ui-control');

ws.onmessage = (event) => {
  const command = JSON.parse(event.data);
  
  if (command.type === 'ui_command') {
    // Execute UI command
    executeUICommand(command);
  } else if (command.type === 'voice_feedback') {
    // Play voice feedback
    speakText(command.text);
  }
};

// Send UI state updates
ws.send(JSON.stringify({
  type: 'ui_state_update',
  currentPage: '/dashboard',
  clientId: 'client123',
  availableElements: getPageElements()
}));
```

## 💾 Data Storage

### Vector Store
The system uses a file-based vector store that creates:
```
./vector-store/
├── client1_embeddings.json
├── client1_metadata.json
├── client2_embeddings.json
├── client2_metadata.json
└── ...
```

### Document Structure
```json
{
  "id": "unique_document_id",
  "content": "Document text content",
  "source": "upload|api|manual",
  "metadata": {
    "filename": "document.pdf",
    "size": 12345,
    "contentType": "application/pdf",
    "uploadedAt": "2024-01-01T00:00:00Z",
    "custom_field": "custom_value"
  }
}
```

## 🎮 Frontend Integration

### React Hook Example
```typescript
// hooks/useVoiceRAG.ts
import { useEffect, useRef, useState } from 'react';

export const useVoiceRAG = (clientId: string, currentContext: string) => {
  const [isConnected, setIsConnected] = useState(false);
  const [lastResponse, setLastResponse] = useState<string>('');
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8080/ws/ui-control');
    wsRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      
      // Send client and context info
      ws.send(JSON.stringify({
        type: 'client_context_update',
        clientId: clientId,
        currentContext: currentContext,
        timestamp: Date.now()
      }));
    };

    ws.onmessage = (event) => {
      const response = JSON.parse(event.data);
      
      if (response.type === 'voice_feedback') {
        setLastResponse(response.text);
        // Optionally use text-to-speech
        speakText(response.text);
      } else if (response.type === 'ui_command') {
        executeUICommand(response);
      }
    };

    return () => ws.close();
  }, [clientId, currentContext]);

  const sendVoiceCommand = async (transcript: string) => {
    try {
      const response = await fetch('/api/voice/command', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          transcript,
          currentContext,
          clientId
        })
      });
      
      const result = await response.json();
      return result;
    } catch (error) {
      console.error('Voice command error:', error);
    }
  };

  return {
    isConnected,
    lastResponse,
    sendVoiceCommand
  };
};
```

### Knowledge Base Management
```typescript
// utils/knowledgeBase.ts
export class KnowledgeBaseManager {
  constructor(private clientId: string) {}

  async addDocument(document: {
    id: string;
    content: string;
    source: string;
    metadata?: Record<string, any>;
  }) {
    const response = await fetch(`/api/rag/clients/${this.clientId}/documents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(document)
    });
    
    return response.json();
  }

  async uploadFiles(files: File[], source = 'upload') {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    formData.append('source', source);

    const response = await fetch(`/api/rag/clients/${this.clientId}/upload`, {
      method: 'POST',
      body: formData
    });
    
    return response.json();
  }

  async getStats() {
    const response = await fetch(`/api/rag/clients/${this.clientId}/stats`);
    return response.json();
  }

  async processVoiceQuery(query: string, context: string) {
    const response = await fetch(`/api/rag/clients/${this.clientId}/voice-command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        voiceCommand: query,
        currentContext: context
      })
    });
    
    return response.json();
  }
}
```

## 📋 Supported Voice Commands

### Navigation Commands
- "Go to dashboard"
- "Navigate to settings"  
- "Open profile page"
- "Take me to contracts"

### UI Interaction Commands
- "Click on submit button"
- "Focus on email field"
- "Type hello world"
- "Clear the input"
- "Scroll to bottom"

### System Commands
- "Clear chat"
- "Delete last message"
- "Enable auto-speak"
- "Show help"

### RAG-Enhanced Queries
- "How do I reset my password?" (searches client knowledge base)
- "What are the contract requirements?" (retrieves relevant documents)
- "Show me the user guide for billing" (context-aware responses)

## 🔍 RAG System Details

### Document Processing Pipeline
1. **Chunking**: Documents split into 500-character chunks with 50-character overlap
2. **Embedding**: Each chunk converted to vector using OpenAI's text-embedding-ada-002
3. **Storage**: Vectors stored in local JSON files with metadata
4. **Retrieval**: Cosine similarity search for relevant chunks
5. **Generation**: GPT-4 generates contextual responses

### Similarity Threshold
- Minimum similarity score: 0.7 (configurable)
- Maximum retrieved chunks: 5 per query
- Cache embeddings to reduce API calls

## 🛠 Development

### Project Structure
```
src/main/java/com/yourcompany/voice/
├── controller/
│   ├── VoiceCommandController.java
│   ├── VoiceInteractionController.java    # NEW: Complete voice-only interface
│   ├── RAGController.java
│   ├── AudioWebSocketHandler.java
│   └── UIControlWebSocketHandler.java
├── service/
│   ├── VoiceInteractionService.java       # NEW: Voice session management
│   ├── RAGService.java
│   ├── VectorStoreService.java
│   ├── CommandService.java
│   ├── LLMCommandService.java
│   └── UIControlService.java
├── config/
└── Application.java
```

### Adding New Voice Commands
1. Update `CommandService.COMMAND_WORDS`
2. Add action mapping in `initializeCommandActions()`
3. Implement command logic method
4. Update UI context mappings in `UIControlService`

### Extending RAG Capabilities
1. Modify chunk size in `RAGService.chunkDocument()`
2. Adjust similarity threshold in `retrieveRelevantDocuments()`
3. Customize system prompt in `buildSystemPrompt()`
4. Add new document metadata fields

## 🚨 Error Handling

The system includes comprehensive error handling:
- Graceful fallback from RAG to standard LLM processing
- API failure recovery mechanisms  
- WebSocket reconnection logic
- Vector store corruption handling

## 📊 Monitoring

### Health Checks
- GET `/actuator/health` - Application health
- GET `/api/rag/stats` - RAG system statistics
- WebSocket connection status monitoring

### Logging
The application logs important events:
- Voice command processing
- RAG query performance
- API call failures
- WebSocket connection events

## 🔐 Security Considerations

- API keys stored as environment variables
- CORS configuration for allowed origins
- Input validation for all endpoints
- Rate limiting recommended for production

## 🚀 Production Deployment

### Recommended Setup
- Use external vector database (Pinecone, Weaviate, etc.)
- Implement Redis for caching embeddings
- Add authentication and authorization
- Set up monitoring and alerting
- Configure load balancing for WebSockets

### Environment Configuration
```yaml
# docker-compose.yml
version: '3.8'
services:
  voice-backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - DEEPGRAM_API_KEY=${DEEPGRAM_API_KEY}
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - ./vector-store:/app/vector-store
```

## 📈 Performance Optimization

- Embed documents asynchronously
- Cache frequently accessed embeddings
- Use connection pooling for HTTP clients
- Implement batch processing for multiple documents
- Consider vector database for large datasets

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Update documentation
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the existing documentation
- Review the WebSocket integration guide (NEXTJS_INTEGRATION.md)

---

**Built with Spring Boot, OpenAI, and modern voice technologies to enable natural conversational interfaces for web applications.** 