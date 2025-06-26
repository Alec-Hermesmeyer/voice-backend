package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages voice sessions, speaker identification, and conversation state
 * Focused responsibility: Session lifecycle and participant management
 */
@Service
public class VoiceSessionService {
    
    private static final Logger logger = Logger.getLogger(VoiceSessionService.class.getName());
    
    @Autowired
    private UserPreferenceService userPreferenceService;
    
    @Autowired
    private SpeakerIdentificationService speakerIdentificationService;
    
    @Autowired
    private ConversationTurnManager conversationTurnManager;
    
    // Active voice sessions
    private final Map<String, VoiceSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Start a new voice session with speaker identification support
     */
    public VoiceSessionResult startSession(String sessionId, String clientId, VoiceSessionConfig config) {
        try {
            VoiceSession session = new VoiceSession(sessionId, clientId, config);
            
            // Initialize speaker identification for this session
            speakerIdentificationService.initializeSession(sessionId, config.getExpectedSpeakers());
            
            // Initialize conversation turn management
            conversationTurnManager.initializeSession(sessionId, config.getConversationMode());
            
            // Load user preferences and personalize session
            UserPreferenceService.UserProfile userProfile = userPreferenceService.getUserProfile(clientId);
            personalizeSession(session, userProfile);
            
            activeSessions.put(sessionId, session);
            
            String welcomeMessage = generatePersonalizedWelcome(config, userProfile);
            
            logger.info("Started voice session: " + sessionId + " for client: " + clientId);
            
            return VoiceSessionResult.success(session, welcomeMessage);
            
        } catch (Exception e) {
            logger.severe("Failed to start voice session: " + e.getMessage());
            return VoiceSessionResult.error("Failed to start voice session: " + e.getMessage());
        }
    }
    
    /**
     * Process voice input with speaker identification and turn management
     */
    public VoiceInputResult processVoiceInput(String sessionId, VoiceInput input) {
        VoiceSession session = activeSessions.get(sessionId);
        if (session == null) {
            return VoiceInputResult.error("No active voice session found");
        }
        
        try {
            session.updateLastActivity();
            
            // Identify speaker from audio input
            SpeakerIdentificationService.SpeakerResult speakerResult = 
                speakerIdentificationService.identifySpeaker(sessionId, input.getAudioData(), input.getTranscript());
            
            // Check if it's this speaker's turn to speak
            ConversationTurnManager.TurnResult turnResult = 
                conversationTurnManager.processTurn(sessionId, speakerResult.getSpeakerId(), input.getTranscript());
            
            if (!turnResult.isValidTurn()) {
                // Handle turn management (e.g., "Please wait your turn" or queue the input)
                return handleInvalidTurn(session, speakerResult, turnResult);
            }
            
            // Update session with speaker and conversation context
            session.setCurrentSpeaker(speakerResult.getSpeakerId());
            session.setCurrentContext(input.getCurrentContext());
            session.addToHistory(new VoiceInteraction(
                input.getTranscript(), 
                speakerResult.getSpeakerId(), 
                input.getCurrentContext(),
                System.currentTimeMillis()
            ));
            
            return VoiceInputResult.success(session, speakerResult, turnResult);
            
        } catch (Exception e) {
            logger.severe("Error processing voice input: " + e.getMessage());
            return VoiceInputResult.error("Error processing voice input: " + e.getMessage());
        }
    }
    
    private VoiceInputResult handleInvalidTurn(VoiceSession session, 
                                              SpeakerIdentificationService.SpeakerResult speakerResult,
                                              ConversationTurnManager.TurnResult turnResult) {
        
        String response = turnResult.getTurnManagementMessage();
        
        // Different responses based on conversation mode
        if (session.getConfig().getConversationMode() == ConversationMode.STRUCTURED) {
            response = "Please wait your turn. " + turnResult.getCurrentSpeaker() + " is speaking.";
        } else if (session.getConfig().getConversationMode() == ConversationMode.OPEN) {
            // In open mode, we might queue the input or allow interruption
            conversationTurnManager.queueInput(session.getSessionId(), speakerResult.getSpeakerId(), turnResult.getInput());
            response = "I'll get back to you in a moment.";
        }
        
        return VoiceInputResult.turnManagement(response, turnResult);
    }
    
    /**
     * End voice session and cleanup
     */
    public void endSession(String sessionId) {
        VoiceSession session = activeSessions.remove(sessionId);
        if (session != null) {
            // Cleanup speaker identification
            speakerIdentificationService.endSession(sessionId);
            
            // Cleanup conversation turn management
            conversationTurnManager.endSession(sessionId);
            
            logger.info("Ended voice session: " + sessionId);
        }
    }
    
    /**
     * Get active session
     */
    public VoiceSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Get session statistics including speaker and conversation data
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        VoiceSession session = activeSessions.get(sessionId);
        if (session == null) {
            return null;
        }
        
        Map<String, Object> stats = Map.of(
            "sessionId", sessionId,
            "clientId", session.getClientId(),
            "startTime", session.getStartTime(),
            "lastActivity", session.getLastActivity(),
            "interactionCount", session.getInteractionCount(),
            "currentSpeaker", session.getCurrentSpeaker(),
            "speakerStats", speakerIdentificationService.getSpeakerStats(sessionId),
            "conversationStats", conversationTurnManager.getConversationStats(sessionId)
        );
        
        return stats;
    }
    
    private void personalizeSession(VoiceSession session, UserPreferenceService.UserProfile userProfile) {
        VoiceSessionConfig config = session.getConfig();
        
        // Apply preferred voice model
        String preferredVoice = (String) userProfile.preferences.get("preferred_voice");
        if (preferredVoice != null) {
            config.setVoiceModel(preferredVoice);
        }
        
        // Apply conversation preferences
        String conversationStyle = (String) userProfile.preferences.get("conversation_style");
        if ("structured".equals(conversationStyle)) {
            config.setConversationMode(ConversationMode.STRUCTURED);
        } else if ("open".equals(conversationStyle)) {
            config.setConversationMode(ConversationMode.OPEN);
        }
    }
    
    private String generatePersonalizedWelcome(VoiceSessionConfig config, UserPreferenceService.UserProfile userProfile) {
        if (config.getWelcomeMessage() != null) {
            return config.getWelcomeMessage();
        }
        
        // Check if returning user with conversation history
        if (userProfile.conversationHistory != null && !userProfile.conversationHistory.isEmpty()) {
            return "Welcome back! I'm ready to help. What would you like to do today?";
        }
        
        // Multi-speaker welcome
        if (config.getExpectedSpeakers() > 1) {
            return "Voice assistant ready for conversation. I can identify different speakers and manage turns. How can I help you today?";
        }
        
        return "Voice assistant ready. How can I help you?";
    }
    
    // Result classes
    
    public static class VoiceSessionResult {
        private final boolean success;
        private final VoiceSession session;
        private final String welcomeMessage;
        private final String errorMessage;
        
        private VoiceSessionResult(boolean success, VoiceSession session, String welcomeMessage, String errorMessage) {
            this.success = success;
            this.session = session;
            this.welcomeMessage = welcomeMessage;
            this.errorMessage = errorMessage;
        }
        
        public static VoiceSessionResult success(VoiceSession session, String welcomeMessage) {
            return new VoiceSessionResult(true, session, welcomeMessage, null);
        }
        
        public static VoiceSessionResult error(String errorMessage) {
            return new VoiceSessionResult(false, null, null, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public VoiceSession getSession() { return session; }
        public String getWelcomeMessage() { return welcomeMessage; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class VoiceInputResult {
        private final boolean success;
        private final VoiceSession session;
        private final SpeakerIdentificationService.SpeakerResult speakerResult;
        private final ConversationTurnManager.TurnResult turnResult;
        private final String errorMessage;
        private final boolean isTurnManagement;
        private final String turnManagementMessage;
        
        private VoiceInputResult(boolean success, VoiceSession session, 
                                SpeakerIdentificationService.SpeakerResult speakerResult,
                                ConversationTurnManager.TurnResult turnResult,
                                String errorMessage, boolean isTurnManagement, 
                                String turnManagementMessage) {
            this.success = success;
            this.session = session;
            this.speakerResult = speakerResult;
            this.turnResult = turnResult;
            this.errorMessage = errorMessage;
            this.isTurnManagement = isTurnManagement;
            this.turnManagementMessage = turnManagementMessage;
        }
        
        public static VoiceInputResult success(VoiceSession session, 
                                              SpeakerIdentificationService.SpeakerResult speakerResult,
                                              ConversationTurnManager.TurnResult turnResult) {
            return new VoiceInputResult(true, session, speakerResult, turnResult, null, false, null);
        }
        
        public static VoiceInputResult error(String errorMessage) {
            return new VoiceInputResult(false, null, null, null, errorMessage, false, null);
        }
        
        public static VoiceInputResult turnManagement(String message, ConversationTurnManager.TurnResult turnResult) {
            return new VoiceInputResult(true, null, null, turnResult, null, true, message);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public VoiceSession getSession() { return session; }
        public SpeakerIdentificationService.SpeakerResult getSpeakerResult() { return speakerResult; }
        public ConversationTurnManager.TurnResult getTurnResult() { return turnResult; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isTurnManagement() { return isTurnManagement; }
        public String getTurnManagementMessage() { return turnManagementMessage; }
    }
}