package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Voice session configuration DTO
 */
public class VoiceSessionConfigDTO {
    
    @JsonProperty("ttsEnabled")
    private boolean ttsEnabled = true;
    
    @JsonProperty("voiceModel")
    private String voiceModel = "alloy";
    
    @JsonProperty("welcomeMessage")
    private String welcomeMessage;
    
    @JsonProperty("expectedSpeakers")
    private int expectedSpeakers = 1;
    
    @JsonProperty("conversationMode")
    private String conversationMode = "SINGLE_SPEAKER";
    
    @JsonProperty("language")
    private String language = "en";
    
    @JsonProperty("enableSpeakerIdentification")
    private boolean enableSpeakerIdentification = false;
    
    @JsonProperty("enableTurnManagement") 
    private boolean enableTurnManagement = false;
    
    // Constructors
    public VoiceSessionConfigDTO() {}
    
    // Getters and setters
    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
    public String getVoiceModel() { return voiceModel; }
    public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public int getExpectedSpeakers() { return expectedSpeakers; }
    public void setExpectedSpeakers(int expectedSpeakers) { this.expectedSpeakers = expectedSpeakers; }
    public String getConversationMode() { return conversationMode; }
    public void setConversationMode(String conversationMode) { this.conversationMode = conversationMode; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isEnableSpeakerIdentification() { return enableSpeakerIdentification; }
    public void setEnableSpeakerIdentification(boolean enableSpeakerIdentification) { this.enableSpeakerIdentification = enableSpeakerIdentification; }
    public boolean isEnableTurnManagement() { return enableTurnManagement; }
    public void setEnableTurnManagement(boolean enableTurnManagement) { this.enableTurnManagement = enableTurnManagement; }
}