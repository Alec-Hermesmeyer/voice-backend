package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for starting a voice session
 */
public class StartVoiceSessionResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("welcomeMessage")
    private String welcomeMessage;
    
    @JsonProperty("config")
    private VoiceSessionConfigDTO config;
    
    @JsonProperty("error")
    private String error;
    
    // Constructors
    public StartVoiceSessionResponse() {}
    
    public StartVoiceSessionResponse(boolean success, String sessionId, String message) {
        this.success = success;
        this.sessionId = sessionId;
        this.message = message;
    }
    
    public static StartVoiceSessionResponse success(String sessionId, String welcomeMessage, VoiceSessionConfigDTO config) {
        StartVoiceSessionResponse response = new StartVoiceSessionResponse(true, sessionId, "Session started successfully");
        response.setWelcomeMessage(welcomeMessage);
        response.setConfig(config);
        return response;
    }
    
    public static StartVoiceSessionResponse error(String error) {
        StartVoiceSessionResponse response = new StartVoiceSessionResponse(false, null, "Failed to start session");
        response.setError(error);
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public VoiceSessionConfigDTO getConfig() { return config; }
    public void setConfig(VoiceSessionConfigDTO config) { this.config = config; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}