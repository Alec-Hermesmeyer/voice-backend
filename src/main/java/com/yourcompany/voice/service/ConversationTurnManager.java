package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Manages conversation turns and flow control for multi-speaker interactions
 * Handles turn-taking, interruptions, and conversation flow
 */
@Service
public class ConversationTurnManager {
    
    private static final Logger logger = Logger.getLogger(ConversationTurnManager.class.getName());
    
    // Session-based conversation data
    private final Map<String, ConversationSession> conversationSessions = new ConcurrentHashMap<>();
    
    /**
     * Initialize conversation turn management for a session
     */
    public void initializeSession(String sessionId, ConversationMode mode) {
        ConversationSession session = new ConversationSession(sessionId, mode);
        conversationSessions.put(sessionId, session);
        
        logger.info("Initialized conversation turn management for session: " + sessionId + 
                   " with mode: " + mode);
    }
    
    /**
     * Process a turn attempt from a speaker
     */
    public TurnResult processTurn(String sessionId, String speakerId, String input) {
        ConversationSession session = conversationSessions.get(sessionId);
        if (session == null) {
            return TurnResult.error("Session not initialized");
        }
        
        try {
            Turn currentTurn = new Turn(speakerId, input, System.currentTimeMillis());
            
            // Check if this speaker can take a turn based on conversation mode
            TurnValidation validation = validateTurn(session, currentTurn);
            
            if (validation.isValid()) {
                // Valid turn - proceed
                session.addTurn(currentTurn);
                session.setCurrentSpeaker(speakerId);
                session.updateLastActivity();
                
                // Check if this completes a conversation exchange
                handleConversationFlow(session, currentTurn);
                
                logger.info("Valid turn for speaker: " + speakerId + " in session: " + sessionId);
                return TurnResult.success(currentTurn, session.getCurrentSpeaker());
                
            } else {
                // Invalid turn - handle based on mode
                return handleInvalidTurn(session, currentTurn, validation);
            }
            
        } catch (Exception e) {
            logger.severe("Error processing turn: " + e.getMessage());
            return TurnResult.error("Turn processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Queue input for later processing (used in open conversation mode)
     */
    public void queueInput(String sessionId, String speakerId, String input) {
        ConversationSession session = conversationSessions.get(sessionId);
        if (session != null) {
            Turn queuedTurn = new Turn(speakerId, input, System.currentTimeMillis());
            session.queueTurn(queuedTurn);
            
            logger.info("Queued input from speaker: " + speakerId + " in session: " + sessionId);
        }
    }
    
    /**
     * Process queued inputs (called when current speaker finishes)
     */
    public List<Turn> processQueuedInputs(String sessionId) {
        ConversationSession session = conversationSessions.get(sessionId);
        if (session == null) {
            return List.of();
        }
        
        List<Turn> processedTurns = new ArrayList<>();
        Turn queuedTurn;
        
        while ((queuedTurn = session.dequeueNextTurn()) != null) {
            // Process the queued turn
            session.addTurn(queuedTurn);
            processedTurns.add(queuedTurn);
            
            logger.info("Processed queued turn from: " + queuedTurn.getSpeakerId());
        }
        
        return processedTurns;
    }
    
    /**
     * Handle conversation flow and determine next actions
     */
    private void handleConversationFlow(ConversationSession session, Turn currentTurn) {
        // Analyze conversation patterns
        ConversationAnalysis analysis = analyzeConversation(session);
        
        // Update conversation state based on analysis
        if (analysis.isQuestionAsked()) {
            session.setWaitingForResponse(true, currentTurn.getSpeakerId());
        } else if (analysis.isResponseGiven() && session.isWaitingForResponse()) {
            session.setWaitingForResponse(false, null);
        }
        
        // Detect conversation endings
        if (analysis.isConversationEnding()) {
            session.setConversationState(ConversationState.ENDING);
        }
        
        // Handle silence detection and prompting
        if (analysis.shouldPromptForResponse()) {
            session.setNeedsPrompt(true);
        }
    }
    
    /**
     * Validate if a speaker can take a turn
     */
    private TurnValidation validateTurn(ConversationSession session, Turn turn) {
        ConversationMode mode = session.getMode();
        String currentSpeaker = session.getCurrentSpeaker();
        String attemptingSpeaker = turn.getSpeakerId();
        
        switch (mode) {
            case STRUCTURED:
                return validateStructuredTurn(session, turn);
                
            case OPEN:
                return validateOpenTurn(session, turn);
                
            case SINGLE_SPEAKER:
                return validateSingleSpeaker(session, turn);
                
            default:
                return TurnValidation.valid();
        }
    }
    
    private TurnValidation validateStructuredTurn(ConversationSession session, Turn turn) {
        String currentSpeaker = session.getCurrentSpeaker();
        String attemptingSpeaker = turn.getSpeakerId();
        
        // First turn is always valid
        if (currentSpeaker == null) {
            return TurnValidation.valid();
        }
        
        // Check if enough time has passed since last turn
        long timeSinceLastTurn = System.currentTimeMillis() - session.getLastTurnTime();
        if (timeSinceLastTurn < 2000) { // 2 second minimum between turns
            return TurnValidation.invalid("Please wait for the current speaker to finish");
        }
        
        // Check if waiting for specific response
        if (session.isWaitingForResponse() && 
            !attemptingSpeaker.equals(session.getExpectedResponder())) {
            return TurnValidation.invalid("Waiting for response from " + session.getExpectedResponder());
        }
        
        return TurnValidation.valid();
    }
    
    private TurnValidation validateOpenTurn(ConversationSession session, Turn turn) {
        // In open mode, most turns are valid but may be queued
        String currentSpeaker = session.getCurrentSpeaker();
        String attemptingSpeaker = turn.getSpeakerId();
        
        // Check for rapid-fire speaking (potential interruption)
        long timeSinceLastTurn = System.currentTimeMillis() - session.getLastTurnTime();
        if (currentSpeaker != null && 
            !currentSpeaker.equals(attemptingSpeaker) && 
            timeSinceLastTurn < 1000) { // 1 second threshold
            
            return TurnValidation.queue("I'll get back to you in a moment");
        }
        
        return TurnValidation.valid();
    }
    
    private TurnValidation validateSingleSpeaker(ConversationSession session, Turn turn) {
        // Only one speaker allowed
        String expectedSpeaker = session.getAuthorizedSpeaker();
        
        if (expectedSpeaker == null) {
            // First speaker becomes the authorized speaker
            session.setAuthorizedSpeaker(turn.getSpeakerId());
            return TurnValidation.valid();
        }
        
        if (!expectedSpeaker.equals(turn.getSpeakerId())) {
            return TurnValidation.invalid("Only " + expectedSpeaker + " is authorized to speak in this session");
        }
        
        return TurnValidation.valid();
    }
    
    /**
     * Handle invalid turn attempts
     */
    private TurnResult handleInvalidTurn(ConversationSession session, Turn turn, TurnValidation validation) {
        switch (validation.getAction()) {
            case REJECT:
                return TurnResult.rejected(validation.getMessage(), session.getCurrentSpeaker());
                
            case QUEUE:
                session.queueTurn(turn);
                return TurnResult.queued(validation.getMessage());
                
            case INTERRUPT:
                // Handle interruption (advanced feature)
                return handleInterruption(session, turn);
                
            default:
                return TurnResult.rejected("Turn not allowed", session.getCurrentSpeaker());
        }
    }
    
    private TurnResult handleInterruption(ConversationSession session, Turn turn) {
        // Advanced interruption handling
        session.addTurn(turn);
        session.setCurrentSpeaker(turn.getSpeakerId());
        
        return TurnResult.interrupted(turn, session.getCurrentSpeaker());
    }
    
    /**
     * Analyze conversation patterns
     */
    private ConversationAnalysis analyzeConversation(ConversationSession session) {
        List<Turn> recentTurns = session.getRecentTurns(5); // Last 5 turns
        ConversationAnalysis analysis = new ConversationAnalysis();
        
        if (recentTurns.isEmpty()) {
            return analysis;
        }
        
        Turn lastTurn = recentTurns.get(recentTurns.size() - 1);
        String lastInput = lastTurn.getInput().toLowerCase();
        
        // Detect questions
        if (lastInput.contains("?") || 
            lastInput.startsWith("what") || 
            lastInput.startsWith("how") || 
            lastInput.startsWith("why") || 
            lastInput.startsWith("when") || 
            lastInput.startsWith("where") || 
            lastInput.startsWith("can you") || 
            lastInput.startsWith("could you") || 
            lastInput.startsWith("would you")) {
            analysis.setQuestionAsked(true);
        }
        
        // Detect conversation endings
        if (lastInput.contains("goodbye") || 
            lastInput.contains("thank you") || 
            lastInput.contains("that's all") || 
            lastInput.contains("end session")) {
            analysis.setConversationEnding(true);
        }
        
        // Detect if response was given to previous question
        if (recentTurns.size() >= 2) {
            Turn previousTurn = recentTurns.get(recentTurns.size() - 2);
            if (isQuestion(previousTurn.getInput()) && !lastTurn.getSpeakerId().equals(previousTurn.getSpeakerId())) {
                analysis.setResponseGiven(true);
            }
        }
        
        // Check if should prompt for response (long silence after question)
        if (session.isWaitingForResponse()) {
            long waitTime = System.currentTimeMillis() - session.getQuestionTime();
            if (waitTime > 10000) { // 10 seconds
                analysis.setShouldPromptForResponse(true);
            }
        }
        
        return analysis;
    }
    
    private boolean isQuestion(String input) {
        String lower = input.toLowerCase();
        return lower.contains("?") || 
               lower.startsWith("what") || 
               lower.startsWith("how") || 
               lower.startsWith("why");
    }
    
    /**
     * Get conversation statistics
     */
    public Map<String, Object> getConversationStats(String sessionId) {
        ConversationSession session = conversationSessions.get(sessionId);
        if (session == null) {
            return Map.of();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("mode", session.getMode().toString());
        stats.put("currentSpeaker", session.getCurrentSpeaker());
        stats.put("totalTurns", session.getTurns().size());
        stats.put("queuedTurns", session.getQueuedTurns().size());
        stats.put("conversationState", session.getConversationState().toString());
        stats.put("speakerStats", session.getSpeakerStats());
        
        return stats;
    }
    
    /**
     * End session and cleanup
     */
    public void endSession(String sessionId) {
        conversationSessions.remove(sessionId);
        logger.info("Ended conversation turn management session: " + sessionId);
    }
    
    // Result and validation classes
    
    public static class TurnResult {
        private final boolean success;
        private final Turn turn;
        private final String currentSpeaker;
        private final String message;
        private final TurnStatus status;
        
        private TurnResult(boolean success, Turn turn, String currentSpeaker, String message, TurnStatus status) {
            this.success = success;
            this.turn = turn;
            this.currentSpeaker = currentSpeaker;
            this.message = message;
            this.status = status;
        }
        
        public static TurnResult success(Turn turn, String currentSpeaker) {
            return new TurnResult(true, turn, currentSpeaker, null, TurnStatus.ACCEPTED);
        }
        
        public static TurnResult rejected(String message, String currentSpeaker) {
            return new TurnResult(false, null, currentSpeaker, message, TurnStatus.REJECTED);
        }
        
        public static TurnResult queued(String message) {
            return new TurnResult(true, null, null, message, TurnStatus.QUEUED);
        }
        
        public static TurnResult interrupted(Turn turn, String currentSpeaker) {
            return new TurnResult(true, turn, currentSpeaker, "Interruption handled", TurnStatus.INTERRUPTED);
        }
        
        public static TurnResult error(String message) {
            return new TurnResult(false, null, null, message, TurnStatus.ERROR);
        }
        
        // Getters
        public boolean isValidTurn() { return success && status == TurnStatus.ACCEPTED; }
        public boolean isSuccess() { return success; }
        public Turn getTurn() { return turn; }
        public String getCurrentSpeaker() { return currentSpeaker; }
        public String getTurnManagementMessage() { return message; }
        public TurnStatus getStatus() { return status; }
        public String getInput() { return turn != null ? turn.getInput() : null; }
    }
    
    private static class TurnValidation {
        private final boolean valid;
        private final String message;
        private final TurnAction action;
        
        private TurnValidation(boolean valid, String message, TurnAction action) {
            this.valid = valid;
            this.message = message;
            this.action = action;
        }
        
        public static TurnValidation valid() {
            return new TurnValidation(true, null, TurnAction.ACCEPT);
        }
        
        public static TurnValidation invalid(String message) {
            return new TurnValidation(false, message, TurnAction.REJECT);
        }
        
        public static TurnValidation queue(String message) {
            return new TurnValidation(false, message, TurnAction.QUEUE);
        }
        
        public static TurnValidation interrupt(String message) {
            return new TurnValidation(false, message, TurnAction.INTERRUPT);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public TurnAction getAction() { return action; }
    }
    
    private static class ConversationAnalysis {
        private boolean questionAsked = false;
        private boolean responseGiven = false;
        private boolean conversationEnding = false;
        private boolean shouldPromptForResponse = false;
        
        // Getters and setters
        public boolean isQuestionAsked() { return questionAsked; }
        public void setQuestionAsked(boolean questionAsked) { this.questionAsked = questionAsked; }
        public boolean isResponseGiven() { return responseGiven; }
        public void setResponseGiven(boolean responseGiven) { this.responseGiven = responseGiven; }
        public boolean isConversationEnding() { return conversationEnding; }
        public void setConversationEnding(boolean conversationEnding) { this.conversationEnding = conversationEnding; }
        public boolean shouldPromptForResponse() { return shouldPromptForResponse; }
        public void setShouldPromptForResponse(boolean shouldPromptForResponse) { this.shouldPromptForResponse = shouldPromptForResponse; }
    }
    
    // Enums
    
    public enum TurnStatus {
        ACCEPTED, REJECTED, QUEUED, INTERRUPTED, ERROR
    }
    
    private enum TurnAction {
        ACCEPT, REJECT, QUEUE, INTERRUPT
    }
    
    public enum ConversationState {
        STARTING, ACTIVE, WAITING_FOR_RESPONSE, ENDING, ENDED
    }
}