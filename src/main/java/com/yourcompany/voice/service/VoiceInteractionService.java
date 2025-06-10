package com.yourcompany.voice.service;

import com.yourcompany.voice.controller.UIControlWebSocketHandler;
import com.yourcompany.voice.service.RAGService.RAGResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONException;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive Voice Interaction Service
 * Provides complete voice-only interface for user interaction
 */
@Service
public class VoiceInteractionService {
    
    private static final String OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech";
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private CommandService commandService;
    
    @Autowired
    private SimpleWakeWordService wakeWordService;
    
    @Autowired
    private UserPreferenceService userPreferenceService;
    
    @Autowired
    private ConversationStateService conversationStateService;
    
    @Autowired
    private ConversationalErrorHandler errorHandler;
    
    @Autowired
    private ContextAwarenessService contextAwarenessService;
    
    @Autowired
    private EmotionalIntelligenceService emotionalIntelligenceService;
    
    @Autowired
    private PredictiveAssistanceService predictiveAssistanceService;
    
    // Active voice sessions
    private final Map<String, VoiceSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Start a complete voice interaction session
     */
    public String startVoiceSession(String sessionId, String clientId, VoiceSessionConfig config) {
        try {
            VoiceSession session = new VoiceSession(sessionId, clientId, config);
            activeSessions.put(sessionId, session);
            
            // Load user preferences and personalize session
            UserPreferenceService.UserProfile userProfile = userPreferenceService.getUserProfile(clientId);
            personalizeSession(session, userProfile);
            
            // Set conversation context for follow-up capabilities
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("sessionStart", true);
            contextData.put("clientId", clientId);
            conversationStateService.setConversationContext(sessionId, "SESSION_START", contextData);
            
            // Send personalized welcome message
            String welcomeMessage = generatePersonalizedWelcome(config, userProfile);
            speakToUser(sessionId, welcomeMessage);
            
            System.out.println("✅ Started voice session for client: " + clientId);
            return "Voice session started successfully";
            
        } catch (Exception e) {
            System.err.println("❌ Error starting voice session: " + e.getMessage());
            return "Failed to start voice session: " + e.getMessage();
        }
    }
    
    /**
     * Process voice input from user
     */
    public CompletableFuture<VoiceResponse> processVoiceInput(String sessionId, String transcript, String currentContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                VoiceSession session = activeSessions.get(sessionId);
                if (session == null) {
                    return new VoiceResponse(false, "No active voice session found", VoiceResponseType.ERROR);
                }
                
                session.updateLastActivity();
                session.setCurrentContext(currentContext);
                
                System.out.println("🎤 Voice input: " + transcript);
                
                // Process the command
                VoiceResponse response = processCommand(session, transcript, currentContext);
                
                // Send audio response
                if (response.isSuccess() && response.getResponseText() != null) {
                    speakToUser(sessionId, response.getResponseText());
                }
                
                // Track user behavior and add to conversation history
                Map<String, Object> behaviorContext = new HashMap<>();
                behaviorContext.put("page", currentContext);
                behaviorContext.put("responseType", response.getType().toString());
                userPreferenceService.trackUserBehavior(session.getClientId(), "voice_interaction", behaviorContext);
                userPreferenceService.addConversationHistory(session.getClientId(), transcript, response.getResponseText(), currentContext);
                
                session.addToHistory(transcript, response.getResponseText());
                conversationStateService.addToConversationHistory(sessionId, transcript + " -> " + response.getResponseText());
                
                return response;
                
            } catch (Exception e) {
                System.err.println("❌ Error processing voice input: " + e.getMessage());
                
                // Handle error conversationally
                Map<String, Object> errorContext = new HashMap<>();
                errorContext.put("page", currentContext);
                errorContext.put("canRetry", true);
                ConversationalErrorHandler.ErrorResponse errorResponse = errorHandler.handleError(
                    sessionId, "SYSTEM_ERROR", transcript, errorContext);
                
                return new VoiceResponse(false, errorResponse.userFriendlyMessage, VoiceResponseType.ERROR);
            }
        });
    }
    
    /**
     * Core command processing logic with enhanced intelligence
     */
    private VoiceResponse processCommand(VoiceSession session, String transcript, String currentContext) {
        try {
            String clientId = session.getClientId();
            String sessionId = session.getSessionId();
            
            // 🧠 ENHANCED CONTEXT AWARENESS - Analyze UI context
            ContextAwarenessService.UIContext uiContext = contextAwarenessService.analyzeContext(sessionId, currentContext);
            
            // 🎭 EMOTIONAL INTELLIGENCE - Analyze user's emotional state
            Map<String, Object> emotionalContext = new HashMap<>();
            emotionalContext.put("page", currentContext);
            emotionalContext.put("errorCount", uiContext.hasErrors ? uiContext.errorCount : 0);
            EmotionalIntelligenceService.EmotionalState emotionalState = 
                emotionalIntelligenceService.analyzeEmotionalState(sessionId, transcript, emotionalContext);
            
            // 🔮 PREDICTIVE ASSISTANCE - Analyze behavior patterns
            PredictiveAssistanceService.PredictiveInsight predictiveInsight = 
                predictiveAssistanceService.analyzeBehaviorAndPredict(sessionId, "voice_command", currentContext);
            
            // Check if this is a follow-up to previous conversation
            if (conversationStateService.isFollowUpInput(sessionId, transcript)) {
                ConversationStateService.FollowUpResponse followUpResponse = 
                    conversationStateService.processFollowUp(sessionId, transcript);
                
                if (followUpResponse != null) {
                    return handleFollowUpResponse(session, followUpResponse, currentContext, emotionalState, uiContext);
                }
            }
            
            // Check for session control commands
            VoiceResponse controlResponse = handleSessionControlCommands(session, transcript);
            if (controlResponse != null) {
                return controlResponse;
            }
            
            // Check for help requests
            if (transcript.toLowerCase().contains("what can you do") || 
                transcript.toLowerCase().contains("help")) {
                return handleCapabilitiesQuery(session);
            }
            
            // Process with RAG if client has knowledge base
            if (clientId != null) {
                RAGResponse ragResponse = ragService.processVoiceCommandWithRAG(clientId, transcript, currentContext);
                
                if (ragResponse.isSuccess() && ragResponse.getRelevantDocuments().size() > 0) {
                    // Set up follow-up context for RAG responses
                    Map<String, Object> contextData = new HashMap<>();
                    contextData.put("ragResponse", ragResponse.getResponse());
                    contextData.put("documentCount", ragResponse.getRelevantDocuments().size());
                    conversationStateService.setConversationContext(sessionId, "RAG_SEARCH", contextData);
                    conversationStateService.addPotentialFollowUps(sessionId, "RAG_SEARCH", Arrays.asList(
                        "Would you like me to explain any specific part in more detail?",
                        "Should I search for related information?"
                    ));
                    
                    // Check if response contains UI actions
                    if (containsUIActions(ragResponse.getResponse())) {
                        executeUIActionsFromResponse(ragResponse.getResponse(), currentContext);
                    }
                    
                    String enhancedResponse = conversationStateService.generateContextualResponse(
                        sessionId, ragResponse.getResponse(), "RAG_SEARCH");
                    
                    // Apply enhanced intelligence to RAG responses
                    enhancedResponse = contextAwarenessService.getContextualResponse(sessionId, enhancedResponse);
                    enhancedResponse = emotionalIntelligenceService.getEmpatheticResponse(emotionalState, enhancedResponse);
                    
                    return new VoiceResponse(true, enhancedResponse, VoiceResponseType.RAG_RESPONSE);
                }
            }
            
            // Fallback to standard command processing
            String commandResult = commandService.processVoiceCommand(transcript, currentContext, clientId);
            
            // Handle command errors conversationally
            if (commandResult.contains("COMMAND_NOT_RECOGNIZED") || commandResult.contains("ERROR")) {
                Map<String, Object> errorContext = new HashMap<>();
                errorContext.put("page", currentContext);
                errorContext.put("canRetry", true);
                ConversationalErrorHandler.ErrorResponse errorResponse = errorHandler.handleError(
                    sessionId, "COMMAND_NOT_RECOGNIZED", transcript, errorContext);
                return new VoiceResponse(false, errorResponse.userFriendlyMessage, VoiceResponseType.ERROR);
            }
            
            String responseText = generateResponseText(commandResult, transcript);
            
            // 🎯 APPLY ENHANCED INTELLIGENCE TO RESPONSE
            
            // 1. Apply Context Awareness
            responseText = contextAwarenessService.getContextualResponse(sessionId, responseText);
            
            // 2. Add Proactive Suggestions (if high confidence)
            List<String> proactiveSuggestions = predictiveAssistanceService.getProactiveSuggestions(sessionId, currentContext);
            if (!proactiveSuggestions.isEmpty() && predictiveInsight.confidence > 70) {
                responseText += " " + getRandomResponse(proactiveSuggestions);
            }
            
            // 3. Apply Emotional Intelligence
            responseText = emotionalIntelligenceService.getEmpatheticResponse(emotionalState, responseText);
            
            // 4. Check for emotional support needs
            if (emotionalIntelligenceService.needsEmotionalSupport(sessionId)) {
                responseText += " If you'd like, I can walk through this step by step to make it easier.";
            }
            
            // Set up follow-up context for navigation commands
            if (commandResult.contains("NAVIGATION") || commandResult.contains("LLM_COMMAND_EXECUTED")) {
                Map<String, Object> contextData = new HashMap<>();
                contextData.put("lastCommand", transcript);
                contextData.put("currentPage", currentContext);
                contextData.put("userEmotion", emotionalState.primaryEmotion);
                contextData.put("uiState", uiContext.dataState);
                conversationStateService.setConversationContext(sessionId, "NAVIGATION", contextData);
                conversationStateService.addPotentialFollowUps(sessionId, "NAVIGATION", null);
                
                responseText = conversationStateService.generateContextualResponse(
                    sessionId, responseText, "NAVIGATION");
            }
            
            System.out.println("🧠 Enhanced Response - Emotion: " + emotionalState.primaryEmotion + 
                             ", Context: " + uiContext.userIntent + 
                             ", Prediction: " + predictiveInsight.predictedNextAction + 
                             " (confidence: " + predictiveInsight.confidence + "%)");
            
            return new VoiceResponse(true, responseText, VoiceResponseType.COMMAND_RESPONSE);
            
        } catch (Exception e) {
            System.err.println("❌ Error in command processing: " + e.getMessage());
            return new VoiceResponse(false, "I encountered an error. Please try again.", VoiceResponseType.ERROR);
        }
    }
    
    /**
     * Handle session control commands
     */
    private VoiceResponse handleSessionControlCommands(VoiceSession session, String transcript) {
        String lower = transcript.toLowerCase().trim();
        
        if (lower.contains("stop listening") || lower.contains("pause")) {
            session.setPaused(true);
            return new VoiceResponse(true, "Voice assistant paused. Say 'resume' to continue.", VoiceResponseType.CONTROL);
        }
        
        if (lower.contains("resume") || lower.contains("continue")) {
            session.setPaused(false);
            return new VoiceResponse(true, "Voice assistant resumed. How can I help?", VoiceResponseType.CONTROL);
        }
        
        if (lower.contains("end session") || lower.contains("goodbye")) {
            endVoiceSession(session.getSessionId());
            return new VoiceResponse(true, "Goodbye! Session ended.", VoiceResponseType.CONTROL);
        }
        
        if (lower.contains("repeat")) {
            String lastResponse = session.getLastResponse();
            if (lastResponse != null) {
                return new VoiceResponse(true, lastResponse, VoiceResponseType.REPEAT);
            } else {
                return new VoiceResponse(true, "I don't have anything to repeat.", VoiceResponseType.CONTROL);
            }
        }
        
        return null;
    }
    
    /**
     * Handle capabilities query
     */
    private VoiceResponse handleCapabilitiesQuery(VoiceSession session) {
        String capabilities = "I can help you navigate the application, " +
            "answer questions about procedures, " +
            "fill out forms, " +
            "and control the interface with voice commands. " +
            "Just tell me what you'd like to do.";
        
        return new VoiceResponse(true, capabilities, VoiceResponseType.HELP);
    }
    
    /**
     * Check if response contains UI actions
     */
    private boolean containsUIActions(String response) {
        String lower = response.toLowerCase();
        return lower.contains("navigate") || lower.contains("click") || 
               lower.contains("go to") || lower.contains("open") ||
               lower.contains("type") || lower.contains("fill");
    }
    
    /**
     * Execute UI actions from response
     */
    private void executeUIActionsFromResponse(String response, String currentContext) {
        try {
            JSONObject actionCommand = extractUIActionFromText(response, currentContext);
            
            if (actionCommand.has("action")) {
                UIControlWebSocketHandler.broadcastUICommand(
                    actionCommand.getString("action"),
                    actionCommand.optString("target", ""),
                    actionCommand.optJSONObject("parameters"),
                    currentContext
                );
                
                System.out.println("🎮 Executed UI action: " + actionCommand.getString("action"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error executing UI actions: " + e.getMessage());
        }
    }
    
    /**
     * Extract UI action from text using LLM
     */
    private JSONObject extractUIActionFromText(String text, String currentContext) throws JSONException, IOException {
        JSONObject requestBody = new JSONObject()
            .put("model", "gpt-4-turbo-preview")
            .put("messages", new org.json.JSONArray()
                .put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Extract UI actions from text. Return JSON with 'action', 'target', and 'parameters' fields."))
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", "Text: " + text + "\nContext: " + currentContext)))
            .put("temperature", 0.1)
            .put("response_format", new JSONObject().put("type", "json_object"));
        
        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + openAiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                String content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                return new JSONObject(content);
            }
        }
        
        return new JSONObject();
    }
    
    /**
     * Generate response text based on command result
     */
    private String generateResponseText(String commandResult, String originalTranscript) {
        switch (commandResult) {
            case "LLM_COMMAND_EXECUTED":
                return generateNavigationResponse(originalTranscript);
            case "SUBMIT":
            case "SEND":
                return getRandomResponse(Arrays.asList(
                    "Form submitted successfully.",
                    "All set! Your form has been submitted.",
                    "Done! I've submitted that for you.",
                    "Perfect! The form is now submitted."
                ));
            case "CLEAR_CHAT":
                return getRandomResponse(Arrays.asList(
                    "Chat cleared.",
                    "All clear! Chat history has been reset.",
                    "Done! Your chat is now cleared."
                ));
            case "ENABLE_AUTOSPEAK":
                return getRandomResponse(Arrays.asList(
                    "Auto-speak enabled.",
                    "Great! I'll now speak responses automatically.",
                    "Auto-speak is now on."
                ));
            case "DISABLE_AUTOSPEAK":
                return getRandomResponse(Arrays.asList(
                    "Auto-speak disabled.",
                    "Got it! I'll be quiet unless you ask me to speak.",
                    "Auto-speak is now off."
                ));
            case "SHOW_HELP":
                return "I can help with navigation, forms, and information lookup.";
            case "LLM_COMMAND_NOT_RECOGNIZED":
                return getRandomResponse(Arrays.asList(
                    "I'm not sure how to help with that. Could you rephrase?",
                    "I didn't quite understand. Can you try saying that differently?",
                    "Let me help you with that. Could you be more specific?",
                    "I want to help, but I'm not sure what you meant. Can you clarify?"
                ));
            default:
                return getRandomResponse(Arrays.asList(
                    "Request processed.",
                    "Done!",
                    "All set!",
                    "Completed."
                ));
        }
    }
    
    /**
     * Generate varied navigation responses
     */
    private String generateNavigationResponse(String originalTranscript) {
        String[] navigationResponses = {
            "Done! I've taken you there.",
            "Perfect! You're now on that page.",
            "All set! I've navigated to that page.",
            "Here we go! I've brought you to that page.",
            "Success! You're now viewing that page.",
            "Great! I've opened that page for you.",
            "There you go! I've navigated to that section.",
            "Done! You should now see that page."
        };
        
        return navigationResponses[new java.util.Random().nextInt(navigationResponses.length)];
    }
    
    /**
     * Helper method to randomly select from response options
     */
    private String getRandomResponse(java.util.List<String> responses) {
        return responses.get(new java.util.Random().nextInt(responses.size()));
    }
    
    /**
     * Convert text to speech and send to user
     */
    public void speakToUser(String sessionId, String text) {
        try {
            VoiceSession session = activeSessions.get(sessionId);
            if (session == null || session.isPaused()) {
                return;
            }
            
            if (!session.getConfig().isTtsEnabled()) {
                UIControlWebSocketHandler.broadcastVoiceFeedback(text, sessionId);
                return;
            }
            
            // Generate TTS audio
            byte[] audioData = generateTTS(text, session.getConfig().getVoiceModel());
            
            // Send audio to frontend
            try {
                JSONObject ttsResponse = new JSONObject()
                    .put("type", "tts_response")
                    .put("text", text)
                    .put("audioData", Base64.getEncoder().encodeToString(audioData))
                    .put("sessionId", sessionId);
                
                UIControlWebSocketHandler.broadcastVoiceFeedback(ttsResponse.toString(), sessionId);
                session.setLastResponse(text);
            } catch (JSONException e) {
                System.err.println("❌ JSON Error in TTS response: " + e.getMessage());
                UIControlWebSocketHandler.broadcastVoiceFeedback(text, sessionId);
            }
            
        } catch (Exception e) {
            System.err.println("❌ TTS Error: " + e.getMessage());
            UIControlWebSocketHandler.broadcastVoiceFeedback(text, sessionId);
        }
    }
    
    /**
     * Generate TTS audio
     */
    private byte[] generateTTS(String text, String voiceModel) throws IOException {
        try {
            JSONObject requestBody = new JSONObject()
                .put("model", "tts-1")
                .put("input", text)
                .put("voice", voiceModel);
            
            Request request = new Request.Builder()
                .url(OPENAI_TTS_URL)
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    System.err.println("❌ OpenAI TTS Error " + response.code() + ": " + errorBody);
                    throw new IOException("TTS API error: " + response.code() + " - " + errorBody);
                }
                return response.body().bytes();
            }
        } catch (JSONException e) {
            throw new IOException("Error creating TTS request JSON", e);
        }
    }
    
    /**
     * End voice session and cleanup enhanced intelligence state
     */
    public void endVoiceSession(String sessionId) {
        VoiceSession session = activeSessions.remove(sessionId);
        if (session != null) {
            // Clean up conversation state and error history
            conversationStateService.clearConversationState(sessionId);
            errorHandler.clearErrorHistory(sessionId);
            
            // Clean up enhanced intelligence state
            emotionalIntelligenceService.clearEmotionalProfile(sessionId);
            predictiveAssistanceService.clearBehaviorPatterns(sessionId);
            
            System.out.println("✅ Ended voice session: " + sessionId);
        }
    }
    
    /**
     * Get session statistics
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        VoiceSession session = activeSessions.get(sessionId);
        if (session != null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("sessionId", sessionId);
            stats.put("clientId", session.getClientId());
            stats.put("startTime", session.getStartTime());
            stats.put("lastActivity", session.getLastActivity());
            stats.put("interactionCount", session.getInteractionCount());
            stats.put("paused", session.isPaused());
            return stats;
        }
        return null;
    }
    
    /**
     * Personalize session based on user preferences
     */
    private void personalizeSession(VoiceSession session, UserPreferenceService.UserProfile userProfile) {
        VoiceSessionConfig config = session.getConfig();
        
        // Apply preferred voice model
        String preferredVoice = (String) userProfile.preferences.get("preferred_voice");
        if (preferredVoice != null) {
            config.setVoiceModel(preferredVoice);
        }
        
        // Apply other preferences as needed
        String responseStyle = (String) userProfile.preferences.get("response_style");
        // Future: Apply response style preferences
    }
    
    /**
     * Generate personalized welcome message
     */
    private String generatePersonalizedWelcome(VoiceSessionConfig config, UserPreferenceService.UserProfile userProfile) {
        if (config.getWelcomeMessage() != null) {
            return config.getWelcomeMessage();
        }
        
        // Check if returning user
        if (userProfile.conversationHistory != null && !userProfile.conversationHistory.isEmpty()) {
            List<String> suggestions = userPreferenceService.getContextualSuggestions(userProfile.clientId, "welcome");
            if (!suggestions.isEmpty()) {
                return "Welcome back! I can help you with " + String.join(", ", suggestions.subList(0, Math.min(2, suggestions.size()))) + 
                       ", or anything else you need. What would you like to do?";
            }
            return "Welcome back! I'm ready to help. What would you like to do today?";
        }
        
        return "Voice assistant ready. How can I help you?";
    }
    
    /**
     * Handle follow-up response from conversation state service with enhanced intelligence
     */
    private VoiceResponse handleFollowUpResponse(VoiceSession session, ConversationStateService.FollowUpResponse followUpResponse, 
                                                String currentContext, EmotionalIntelligenceService.EmotionalState emotionalState, 
                                                ContextAwarenessService.UIContext uiContext) {
        String responseText;
        
        switch (followUpResponse.intent) {
            case "CONTINUE":
                // User wants more information
                responseText = followUpResponse.suggestion;
                if (followUpResponse.originalContext.equals("RAG_SEARCH")) {
                    // Get more detailed RAG response
                    String ragResponse = (String) followUpResponse.contextData.get("ragResponse");
                    responseText = "Let me provide more details: " + ragResponse;
                }
                break;
                
            case "SPECIFIC":
                // User has a specific request
                responseText = "I'll help you with " + followUpResponse.specificRequest + ". Let me search for that information.";
                // Could trigger new RAG search here with specific request
                break;
                
            case "STOP":
                // User wants to stop the current topic
                responseText = followUpResponse.suggestion;
                conversationStateService.clearConversationState(session.getSessionId());
                break;
                
            case "CLARIFY":
            default:
                responseText = followUpResponse.suggestion;
                break;
        }
        
        // Apply enhanced intelligence to follow-up responses
        responseText = contextAwarenessService.getContextualResponse(session.getSessionId(), responseText);
        responseText = emotionalIntelligenceService.getEmpatheticResponse(emotionalState, responseText);
        
        // Add context-specific help if user seems confused
        if ("confused".equals(emotionalState.primaryEmotion) && uiContext.hasErrors) {
            responseText += " I notice there are some issues on this page. Would you like me to help address them?";
        }
        
        return new VoiceResponse(true, responseText, VoiceResponseType.RAG_RESPONSE);
    }
    
    // Inner classes
    
    public static class VoiceSession {
        private final String sessionId;
        private final String clientId;
        private final VoiceSessionConfig config;
        private final long startTime;
        private long lastActivity;
        private String currentContext;
        private boolean paused;
        private int interactionCount;
        private String lastResponse;
        private final List<VoiceInteraction> history;
        
        public VoiceSession(String sessionId, String clientId, VoiceSessionConfig config) {
            this.sessionId = sessionId;
            this.clientId = clientId;
            this.config = config;
            this.startTime = System.currentTimeMillis();
            this.lastActivity = startTime;
            this.paused = false;
            this.interactionCount = 0;
            this.history = new ArrayList<>();
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
            this.interactionCount++;
        }
        
        public void addToHistory(String input, String response) {
            history.add(new VoiceInteraction(input, response, System.currentTimeMillis()));
            if (history.size() > 50) {
                history.remove(0);
            }
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getClientId() { return clientId; }
        public VoiceSessionConfig getConfig() { return config; }
        public long getStartTime() { return startTime; }
        public long getLastActivity() { return lastActivity; }
        public String getCurrentContext() { return currentContext; }
        public void setCurrentContext(String currentContext) { this.currentContext = currentContext; }
        public boolean isPaused() { return paused; }
        public void setPaused(boolean paused) { this.paused = paused; }
        public int getInteractionCount() { return interactionCount; }
        public String getLastResponse() { return lastResponse; }
        public void setLastResponse(String lastResponse) { this.lastResponse = lastResponse; }
        public List<VoiceInteraction> getHistory() { return history; }
    }
    
    public static class VoiceSessionConfig {
        private boolean ttsEnabled = true;
        private String voiceModel = "alloy";
        private String welcomeMessage;
        
        public boolean isTtsEnabled() { return ttsEnabled; }
        public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
        public String getVoiceModel() { return voiceModel; }
        public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
        public String getWelcomeMessage() { return welcomeMessage; }
        public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    }
    
    public static class VoiceResponse {
        private final boolean success;
        private final String responseText;
        private final VoiceResponseType type;
        
        public VoiceResponse(boolean success, String responseText, VoiceResponseType type) {
            this.success = success;
            this.responseText = responseText;
            this.type = type;
        }
        
        public boolean isSuccess() { return success; }
        public String getResponseText() { return responseText; }
        public VoiceResponseType getType() { return type; }
    }
    
    public enum VoiceResponseType {
        RAG_RESPONSE, COMMAND_RESPONSE, CONTROL, HELP, REPEAT, ERROR
    }
    
    public static class VoiceInteraction {
        private final String input;
        private final String response;
        private final long timestamp;
        
        public VoiceInteraction(String input, String response, long timestamp) {
            this.input = input;
            this.response = response;
            this.timestamp = timestamp;
        }
        
        public String getInput() { return input; }
        public String getResponse() { return response; }
        public long getTimestamp() { return timestamp; }
    }
} 