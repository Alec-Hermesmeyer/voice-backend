# Voice-Only Interface Integration Guide

This guide shows how to integrate the **Voice Interaction Service** to create a complete hands-free, voice-only experience for your users.

## Overview

The Voice Interaction Service provides:
- **Complete voice-only operation** - Users can interact with your app using only their voice
- **Smart RAG integration** - Context-aware responses from your knowledge base
- **Real-time TTS feedback** - Immediate audio responses to user input
- **UI control via voice** - Navigate and control the interface through voice commands
- **Session management** - Persistent voice sessions with conversation history

## Quick Start

### 1. Start a Voice Session

```javascript
// Initialize voice-only session
const startVoiceSession = async (clientId) => {
  const sessionId = `voice-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  
  const response = await fetch('/api/voice-interaction/sessions/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: sessionId,
      clientId: clientId,
      ttsEnabled: true,
      voiceModel: 'alloy', // or: echo, fable, onyx, nova, shimmer
      welcomeMessage: 'Welcome! I can help you navigate and control this application using only your voice. What would you like to do?'
    })
  });
  
  const result = await response.json();
  console.log('Voice session started:', result);
  return sessionId;
};
```

### 2. Implement Speech Recognition

```javascript
class VoiceOnlyInterface {
  constructor(sessionId, clientId) {
    this.sessionId = sessionId;
    this.clientId = clientId;
    this.isListening = false;
    this.recognition = null;
    this.audioContext = null;
    
    this.initSpeechRecognition();
    this.initWebSocket();
  }
  
  initSpeechRecognition() {
    // Use Web Speech API for continuous listening
    this.recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
    this.recognition.continuous = true;
    this.recognition.interimResults = false;
    this.recognition.lang = 'en-US';
    
    this.recognition.onresult = (event) => {
      const transcript = event.results[event.resultIndex][0].transcript.trim();
      this.processVoiceInput(transcript);
    };
    
    this.recognition.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
      if (event.error === 'no-speech') {
        this.restartListening();
      }
    };
    
    this.recognition.onend = () => {
      if (this.isListening) {
        this.restartListening();
      }
    };
  }
  
  startListening() {
    this.isListening = true;
    this.recognition.start();
    console.log('🎤 Voice-only mode activated - listening...');
  }
  
  stopListening() {
    this.isListening = false;
    this.recognition.stop();
    console.log('🔇 Voice listening stopped');
  }
  
  restartListening() {
    if (this.isListening) {
      setTimeout(() => {
        this.recognition.start();
      }, 100);
    }
  }
  
  async processVoiceInput(transcript) {
    console.log('🎤 User said:', transcript);
    
    // Get current page context
    const currentContext = this.getCurrentContext();
    
    // Send to backend for processing
    const response = await fetch(`/api/voice-interaction/sessions/${this.sessionId}/input`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transcript: transcript,
        currentContext: currentContext
      })
    });
    
    const result = await response.json();
    console.log('🤖 Voice response:', result);
    
    // Audio response is sent via WebSocket
    // UI actions are executed automatically
  }
  
  getCurrentContext() {
    return {
      url: window.location.href,
      page: window.location.pathname,
      title: document.title,
      activeElement: document.activeElement?.tagName,
      forms: Array.from(document.forms).map(form => ({
        id: form.id,
        name: form.name,
        action: form.action
      })),
      buttons: Array.from(document.querySelectorAll('button')).map(btn => ({
        text: btn.textContent.trim(),
        id: btn.id,
        className: btn.className
      })),
      links: Array.from(document.querySelectorAll('a')).map(link => ({
        text: link.textContent.trim(),
        href: link.href,
        id: link.id
      }))
    };
  }
  
  initWebSocket() {
    this.ws = new WebSocket(`ws://localhost:8080/ui-control`);
    
    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'tts_response') {
          this.playAudioResponse(data);
        } else if (data.type === 'ui_command') {
          this.executeUICommand(data);
        }
        
      } catch (e) {
        console.error('WebSocket message error:', e);
      }
    };
  }
  
  async playAudioResponse(ttsData) {
    try {
      // Convert base64 audio to blob and play
      const audioData = atob(ttsData.audioData);
      const arrayBuffer = new ArrayBuffer(audioData.length);
      const uint8Array = new Uint8Array(arrayBuffer);
      
      for (let i = 0; i < audioData.length; i++) {
        uint8Array[i] = audioData.charCodeAt(i);
      }
      
      const audioBlob = new Blob([arrayBuffer], { type: 'audio/mpeg' });
      const audioUrl = URL.createObjectURL(audioBlob);
      
      const audio = new Audio(audioUrl);
      await audio.play();
      
      console.log('🔊 Played TTS response:', ttsData.text);
      
    } catch (error) {
      console.error('Error playing TTS audio:', error);
      // Fallback: use speech synthesis API
      this.fallbackTTS(ttsData.text);
    }
  }
  
  fallbackTTS(text) {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    speechSynthesis.speak(utterance);
  }
  
  executeUICommand(command) {
    const { action, target, parameters } = command;
    
    switch (action) {
      case 'navigate':
        if (parameters?.url) {
          window.location.href = parameters.url;
        }
        break;
        
      case 'click':
        const element = this.findElement(target, parameters);
        if (element) {
          element.click();
          console.log('✅ Clicked:', target);
        }
        break;
        
      case 'type':
        const input = this.findElement(target, parameters);
        if (input && parameters?.text) {
          input.value = parameters.text;
          input.dispatchEvent(new Event('input', { bubbles: true }));
          console.log('✅ Typed into:', target);
        }
        break;
        
      case 'focus':
        const focusElement = this.findElement(target, parameters);
        if (focusElement) {
          focusElement.focus();
          console.log('✅ Focused:', target);
        }
        break;
        
      case 'scroll':
        if (parameters?.direction === 'up') {
          window.scrollBy(0, -300);
        } else if (parameters?.direction === 'down') {
          window.scrollBy(0, 300);
        }
        break;
    }
  }
  
  findElement(target, parameters) {
    // Try multiple selection strategies
    if (parameters?.id) {
      return document.getElementById(parameters.id);
    }
    
    if (parameters?.className) {
      return document.querySelector(`.${parameters.className}`);
    }
    
    if (target) {
      // Try by text content
      const elements = Array.from(document.querySelectorAll('*'));
      return elements.find(el => 
        el.textContent?.trim().toLowerCase().includes(target.toLowerCase())
      );
    }
    
    return null;
  }
}
```

### 3. Initialize Voice-Only Mode

```javascript
// Complete voice-only application setup
class VoiceOnlyApp {
  constructor(clientId) {
    this.clientId = clientId;
    this.voiceInterface = null;
    this.sessionId = null;
  }
  
  async initialize() {
    try {
      // Start voice session
      this.sessionId = await startVoiceSession(this.clientId);
      
      // Initialize voice interface
      this.voiceInterface = new VoiceOnlyInterface(this.sessionId, this.clientId);
      
      // Start listening immediately
      this.voiceInterface.startListening();
      
      // Add voice controls
      this.addVoiceControls();
      
      console.log('🎙️ Voice-only mode ready!');
      
    } catch (error) {
      console.error('Failed to initialize voice-only mode:', error);
    }
  }
  
  addVoiceControls() {
    // Add voice status indicator
    const indicator = document.createElement('div');
    indicator.id = 'voice-indicator';
    indicator.innerHTML = '🎤 Voice Active';
    indicator.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: #4CAF50;
      color: white;
      padding: 10px 15px;
      border-radius: 20px;
      font-family: Arial, sans-serif;
      font-size: 14px;
      z-index: 9999;
      box-shadow: 0 2px 10px rgba(0,0,0,0.3);
    `;
    document.body.appendChild(indicator);
    
    // Add keyboard shortcuts for voice control
    document.addEventListener('keydown', (e) => {
      if (e.ctrlKey && e.key === 'm') { // Ctrl+M to toggle
        e.preventDefault();
        this.toggleVoiceMode();
      }
    });
  }
  
  toggleVoiceMode() {
    if (this.voiceInterface.isListening) {
      this.voiceInterface.stopListening();
      document.getElementById('voice-indicator').textContent = '🔇 Voice Paused';
      document.getElementById('voice-indicator').style.background = '#FF9800';
    } else {
      this.voiceInterface.startListening();
      document.getElementById('voice-indicator').textContent = '🎤 Voice Active';
      document.getElementById('voice-indicator').style.background = '#4CAF50';
    }
  }
  
  async endSession() {
    if (this.sessionId) {
      await fetch(`/api/voice-interaction/sessions/${this.sessionId}/end`, {
        method: 'POST'
      });
      
      this.voiceInterface.stopListening();
      document.getElementById('voice-indicator')?.remove();
      
      console.log('Voice session ended');
    }
  }
}
```

### 4. Usage Examples

```javascript
// Initialize for different scenarios

// Healthcare Application
const healthcareApp = new VoiceOnlyApp('healthcare-client');
await healthcareApp.initialize();

// Users can now say:
// "Navigate to patient records"
// "Fill in patient name as John Smith"
// "What is the medication protocol for diabetes?"
// "Submit this form"
// "Go back to main menu"

// Financial Services Application
const financeApp = new VoiceOnlyApp('finance-client');
await financeApp.initialize();

// Users can now say:
// "Show me account balance"
// "Transfer money to savings account"
// "What are the current interest rates?"
// "Open investment portfolio"
// "Schedule a meeting with advisor"

// E-commerce Application
const ecommerceApp = new VoiceOnlyApp('ecommerce-client');
await ecommerceApp.initialize();

// Users can now say:
// "Search for wireless headphones"
// "Add this item to cart"
// "What is the return policy?"
// "Proceed to checkout"
// "Apply discount code SAVE20"
```

## Voice Commands

### Navigation Commands
- "Go to [page name]"
- "Navigate to [section]"
- "Open [menu item]"
- "Go back"
- "Go to main page"
- "Show dashboard"

### Form Interaction
- "Fill in [field name] as [value]"
- "Type [text] in [field]"
- "Select [option] from dropdown"
- "Check the [checkbox name]"
- "Submit form"
- "Clear form"

### Information Queries
- "What is [topic]?"
- "How do I [task]?"
- "Explain [concept]"
- "What are the requirements for [process]?"
- "Show me help for [feature]"

### Interface Control
- "Click [button name]"
- "Scroll down"
- "Scroll up"
- "Focus on [element]"
- "Clear chat"
- "Repeat that"

### Session Control
- "Pause listening"
- "Resume listening"
- "End session"
- "What can you do?"
- "Help"

## Advanced Features

### Custom Voice Commands

```javascript
// Add custom voice command handlers
voiceInterface.addCustomCommand('take screenshot', () => {
  // Custom screenshot functionality
  html2canvas(document.body).then(canvas => {
    // Handle screenshot
  });
});

voiceInterface.addCustomCommand('export data', () => {
  // Custom data export
  const data = gatherFormData();
  downloadAsCSV(data);
});
```

### Voice Feedback Customization

```javascript
// Customize TTS voice and responses
const voiceConfig = {
  voiceModel: 'nova', // Female voice
  customResponses: {
    'form_submitted': 'Great! Your form has been submitted successfully.',
    'navigation_complete': 'You are now on the [page] page.',
    'action_completed': 'Done! I have completed that action for you.'
  }
};
```

### Multi-language Support

```javascript
// Initialize with different language
voiceInterface.recognition.lang = 'es-ES'; // Spanish
voiceInterface.recognition.lang = 'fr-FR'; // French
voiceInterface.recognition.lang = 'de-DE'; // German
```

## Production Deployment

### Performance Optimization

```javascript
// Optimize for production
const productionConfig = {
  // Reduce API calls with request debouncing
  debounceMs: 300,
  
  // Cache frequent responses
  enableResponseCache: true,
  
  // Batch UI commands
  batchUICommands: true,
  
  // Audio compression
  audioFormat: 'opus',
  audioQuality: 'medium'
};
```

### Error Handling

```javascript
// Robust error handling
voiceInterface.onError = (error) => {
  console.error('Voice interface error:', error);
  
  // Fallback to text input
  if (error.type === 'speech_recognition_failed') {
    showTextInputFallback();
  }
  
  // Retry mechanism
  if (error.type === 'network_error') {
    setTimeout(() => voiceInterface.retry(), 2000);
  }
};
```

### Analytics and Monitoring

```javascript
// Track voice interaction metrics
voiceInterface.onInteraction = (data) => {
  analytics.track('voice_command', {
    command: data.transcript,
    success: data.success,
    responseTime: data.responseTime,
    sessionId: data.sessionId
  });
};
```

## Browser Compatibility

- **Chrome/Edge**: Full support with Web Speech API
- **Firefox**: Partial support (requires polyfill for some features)
- **Safari**: Limited support (manual speech recognition trigger required)
- **Mobile**: Best on Chrome for Android, limited on iOS Safari

## Security Considerations

- Voice data is processed securely through OpenAI APIs
- No voice recordings are stored permanently
- Client-specific knowledge bases are isolated
- CORS properly configured for frontend domains
- Session tokens expire automatically

## Troubleshooting

### Common Issues

1. **Microphone not working**: Check browser permissions
2. **TTS not playing**: Verify audio autoplay policies
3. **Commands not recognized**: Check microphone quality and background noise
4. **WebSocket connection failed**: Verify backend is running and accessible
5. **RAG responses not working**: Ensure client knowledge base is initialized

Your voice-only interface is now ready! Users can interact with your application completely hands-free using natural voice commands. 