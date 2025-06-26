package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Turn management information DTO
 */
public class TurnInfoDTO {
    
    @JsonProperty("turnAllowed")
    private boolean turnAllowed;
    
    @JsonProperty("currentSpeaker")
    private String currentSpeaker;
    
    @JsonProperty("turnMessage")
    private String turnMessage;
    
    @JsonProperty("queuePosition")
    private Integer queuePosition;
    
    @JsonProperty("conversationState")
    private String conversationState;
    
    // Constructors
    public TurnInfoDTO() {}
    
    public TurnInfoDTO(boolean turnAllowed, String currentSpeaker, String turnMessage) {
        this.turnAllowed = turnAllowed;
        this.currentSpeaker = currentSpeaker;
        this.turnMessage = turnMessage;
    }
    
    // Getters and setters
    public boolean isTurnAllowed() { return turnAllowed; }
    public void setTurnAllowed(boolean turnAllowed) { this.turnAllowed = turnAllowed; }
    public String getCurrentSpeaker() { return currentSpeaker; }
    public void setCurrentSpeaker(String currentSpeaker) { this.currentSpeaker = currentSpeaker; }
    public String getTurnMessage() { return turnMessage; }
    public void setTurnMessage(String turnMessage) { this.turnMessage = turnMessage; }
    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }
    public String getConversationState() { return conversationState; }
    public void setConversationState(String conversationState) { this.conversationState = conversationState; }
}