package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request to process voice input
 */
public class ProcessVoiceInputRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("audioData")
    private String audioData; // Base64 encoded
    
    @JsonProperty("transcript")
    private String transcript; // Optional pre-transcribed text
    
    @JsonProperty("currentContext")
    private String currentContext;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public ProcessVoiceInputRequest() {}
    
    public ProcessVoiceInputRequest(String sessionId, String audioData, String currentContext) {
        this.sessionId = sessionId;
        this.audioData = audioData;
        this.currentContext = currentContext;
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAudioData() { return audioData; }
    public void setAudioData(String audioData) { this.audioData = audioData; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public String getCurrentContext() { return currentContext; }
    public void setCurrentContext(String currentContext) { this.currentContext = currentContext; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}