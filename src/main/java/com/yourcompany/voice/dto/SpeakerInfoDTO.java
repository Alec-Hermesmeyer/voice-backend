package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Speaker information DTO
 */
public class SpeakerInfoDTO {
    
    @JsonProperty("speakerId")
    private String speakerId;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("isNewSpeaker")
    private boolean isNewSpeaker;
    
    @JsonProperty("speakerName")
    private String speakerName;
    
    // Constructors
    public SpeakerInfoDTO() {}
    
    public SpeakerInfoDTO(String speakerId, double confidence, boolean isNewSpeaker) {
        this.speakerId = speakerId;
        this.confidence = confidence;
        this.isNewSpeaker = isNewSpeaker;
    }
    
    // Getters and setters
    public String getSpeakerId() { return speakerId; }
    public void setSpeakerId(String speakerId) { this.speakerId = speakerId; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isNewSpeaker() { return isNewSpeaker; }
    public void setNewSpeaker(boolean newSpeaker) { isNewSpeaker = newSpeaker; }
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
}