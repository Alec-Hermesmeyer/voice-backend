package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Session statistics DTO
 */
public class SessionStatsDTO {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("startTime")
    private long startTime;
    
    @JsonProperty("lastActivity")
    private long lastActivity;
    
    @JsonProperty("interactionCount")
    private int interactionCount;
    
    @JsonProperty("currentSpeaker")
    private String currentSpeaker;
    
    @JsonProperty("speakerStats")
    private Map<String, Object> speakerStats;
    
    @JsonProperty("conversationStats")
    private Map<String, Object> conversationStats;
    
    @JsonProperty("errorStats")
    private Map<String, Object> errorStats;
    
    // Constructors
    public SessionStatsDTO() {}
    
    public SessionStatsDTO(String sessionId, String clientId, long startTime, int interactionCount) {
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.startTime = startTime;
        this.interactionCount = interactionCount;
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getLastActivity() { return lastActivity; }
    public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
    public int getInteractionCount() { return interactionCount; }
    public void setInteractionCount(int interactionCount) { this.interactionCount = interactionCount; }
    public String getCurrentSpeaker() { return currentSpeaker; }
    public void setCurrentSpeaker(String currentSpeaker) { this.currentSpeaker = currentSpeaker; }
    public Map<String, Object> getSpeakerStats() { return speakerStats; }
    public void setSpeakerStats(Map<String, Object> speakerStats) { this.speakerStats = speakerStats; }
    public Map<String, Object> getConversationStats() { return conversationStats; }
    public void setConversationStats(Map<String, Object> conversationStats) { this.conversationStats = conversationStats; }
    public Map<String, Object> getErrorStats() { return errorStats; }
    public void setErrorStats(Map<String, Object> errorStats) { this.errorStats = errorStats; }
}