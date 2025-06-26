package com.yourcompany.voice.service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Voice session data model with speaker identification and conversation support
 */
public class VoiceSession {
    private final String sessionId;
    private final String clientId;
    private final VoiceSessionConfig config;
    private final long startTime;
    private long lastActivity;
    private String currentContext;
    private String currentSpeaker;
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
    
    public void addToHistory(VoiceInteraction interaction) {
        history.add(interaction);
        if (history.size() > 100) { // Keep last 100 interactions
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
    public String getCurrentSpeaker() { return currentSpeaker; }
    public void setCurrentSpeaker(String currentSpeaker) { this.currentSpeaker = currentSpeaker; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public int getInteractionCount() { return interactionCount; }
    public String getLastResponse() { return lastResponse; }
    public void setLastResponse(String lastResponse) { this.lastResponse = lastResponse; }
    public List<VoiceInteraction> getHistory() { return history; }
}

/**
 * Voice session configuration with conversation and speaker settings
 */
class VoiceSessionConfig {
    private boolean ttsEnabled = true;
    private String voiceModel = "alloy";
    private String welcomeMessage;
    private int expectedSpeakers = 1;
    private ConversationMode conversationMode = ConversationMode.SINGLE_SPEAKER;
    
    // Getters and setters
    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
    public String getVoiceModel() { return voiceModel; }
    public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public int getExpectedSpeakers() { return expectedSpeakers; }
    public void setExpectedSpeakers(int expectedSpeakers) { this.expectedSpeakers = expectedSpeakers; }
    public ConversationMode getConversationMode() { return conversationMode; }
    public void setConversationMode(ConversationMode conversationMode) { this.conversationMode = conversationMode; }
}

/**
 * Voice interaction with speaker and context information
 */
class VoiceInteraction {
    private final String input;
    private final String response;
    private final String speakerId;
    private final String context;
    private final long timestamp;
    
    public VoiceInteraction(String input, String speakerId, String context, long timestamp) {
        this.input = input;
        this.response = null;
        this.speakerId = speakerId;
        this.context = context;
        this.timestamp = timestamp;
    }
    
    public VoiceInteraction(String input, String response, String speakerId, String context, long timestamp) {
        this.input = input;
        this.response = response;
        this.speakerId = speakerId;
        this.context = context;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getInput() { return input; }
    public String getResponse() { return response; }
    public String getSpeakerId() { return speakerId; }
    public String getContext() { return context; }
    public long getTimestamp() { return timestamp; }
}

/**
 * Voice input data model
 */
class VoiceInput {
    private final byte[] audioData;
    private final String transcript;
    private final String currentContext;
    
    public VoiceInput(byte[] audioData, String transcript, String currentContext) {
        this.audioData = audioData;
        this.transcript = transcript;
        this.currentContext = currentContext;
    }
    
    // Getters
    public byte[] getAudioData() { return audioData; }
    public String getTranscript() { return transcript; }
    public String getCurrentContext() { return currentContext; }
}

/**
 * Conversation modes for different interaction types
 */
enum ConversationMode {
    SINGLE_SPEAKER,    // Only one speaker allowed
    STRUCTURED,        // Turn-taking with rules
    OPEN              // Free-flowing conversation
}

/**
 * Conversation session for turn management
 */
class ConversationSession {
    private final String sessionId;
    private final ConversationMode mode;
    private final List<Turn> turns = new ArrayList<>();
    private final Queue<Turn> queuedTurns = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> speakerTurnCounts = new HashMap<>();
    
    private String currentSpeaker;
    private String authorizedSpeaker;
    private long lastTurnTime;
    private long lastActivity;
    private ConversationTurnManager.ConversationState conversationState = ConversationTurnManager.ConversationState.STARTING;
    
    // Response waiting state
    private boolean waitingForResponse = false;
    private String expectedResponder;
    private long questionTime;
    private boolean needsPrompt = false;
    
    public ConversationSession(String sessionId, ConversationMode mode) {
        this.sessionId = sessionId;
        this.mode = mode;
        this.lastActivity = System.currentTimeMillis();
        this.lastTurnTime = lastActivity;
    }
    
    public void addTurn(Turn turn) {
        turns.add(turn);
        speakerTurnCounts.merge(turn.getSpeakerId(), 1, Integer::sum);
        lastTurnTime = turn.getTimestamp();
        updateLastActivity();
        
        if (conversationState == ConversationTurnManager.ConversationState.STARTING) {
            conversationState = ConversationTurnManager.ConversationState.ACTIVE;
        }
    }
    
    public void queueTurn(Turn turn) {
        queuedTurns.offer(turn);
    }
    
    public Turn dequeueNextTurn() {
        return queuedTurns.poll();
    }
    
    public List<Turn> getRecentTurns(int count) {
        int size = turns.size();
        int fromIndex = Math.max(0, size - count);
        return turns.subList(fromIndex, size);
    }
    
    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
    
    public void setWaitingForResponse(boolean waiting, String expectedResponder) {
        this.waitingForResponse = waiting;
        this.expectedResponder = expectedResponder;
        if (waiting) {
            this.questionTime = System.currentTimeMillis();
        }
    }
    
    public Map<String, Object> getSpeakerStats() {
        Map<String, Object> stats = new HashMap<>();
        for (Map.Entry<String, Integer> entry : speakerTurnCounts.entrySet()) {
            stats.put(entry.getKey(), Map.of(
                "turnCount", entry.getValue(),
                "percentage", (double) entry.getValue() / turns.size() * 100
            ));
        }
        return stats;
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public ConversationMode getMode() { return mode; }
    public List<Turn> getTurns() { return turns; }
    public Queue<Turn> getQueuedTurns() { return queuedTurns; }
    public String getCurrentSpeaker() { return currentSpeaker; }
    public void setCurrentSpeaker(String currentSpeaker) { this.currentSpeaker = currentSpeaker; }
    public String getAuthorizedSpeaker() { return authorizedSpeaker; }
    public void setAuthorizedSpeaker(String authorizedSpeaker) { this.authorizedSpeaker = authorizedSpeaker; }
    public long getLastTurnTime() { return lastTurnTime; }
    public long getLastActivity() { return lastActivity; }
    public ConversationTurnManager.ConversationState getConversationState() { return conversationState; }
    public void setConversationState(ConversationTurnManager.ConversationState conversationState) { this.conversationState = conversationState; }
    public boolean isWaitingForResponse() { return waitingForResponse; }
    public String getExpectedResponder() { return expectedResponder; }
    public long getQuestionTime() { return questionTime; }
    public boolean isNeedsPrompt() { return needsPrompt; }
    public void setNeedsPrompt(boolean needsPrompt) { this.needsPrompt = needsPrompt; }
}

/**
 * Individual conversation turn
 */
class Turn {
    private final String speakerId;
    private final String input;
    private final long timestamp;
    
    public Turn(String speakerId, String input, long timestamp) {
        this.speakerId = speakerId;
        this.input = input;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSpeakerId() { return speakerId; }
    public String getInput() { return input; }
    public long getTimestamp() { return timestamp; }
}