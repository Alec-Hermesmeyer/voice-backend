package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to start a voice session
 */
public class StartVoiceSessionRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("clientId") 
    private String clientId;
    
    @JsonProperty("config")
    private VoiceSessionConfigDTO config;
    
    // Constructors
    public StartVoiceSessionRequest() {}
    
    public StartVoiceSessionRequest(String sessionId, String clientId, VoiceSessionConfigDTO config) {
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.config = config;
    }
    
    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public VoiceSessionConfigDTO getConfig() { return config; }
    public void setConfig(VoiceSessionConfigDTO config) { this.config = config; }
}