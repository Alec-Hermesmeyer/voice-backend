package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;

/**
 * Request to enroll a speaker
 */
public class EnrollSpeakerRequest {
    
    @JsonProperty("speakerId")
    private String speakerId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("audioSamples")
    private List<String> audioSamples; // Base64 encoded audio samples
    
    @JsonProperty("speakerName")
    private String speakerName;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public EnrollSpeakerRequest() {}
    
    public EnrollSpeakerRequest(String speakerId, String sessionId, List<String> audioSamples) {
        this.speakerId = speakerId;
        this.sessionId = sessionId;
        this.audioSamples = audioSamples;
    }
    
    // Getters and setters
    public String getSpeakerId() { return speakerId; }
    public void setSpeakerId(String speakerId) { this.speakerId = speakerId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<String> getAudioSamples() { return audioSamples; }
    public void setAudioSamples(List<String> audioSamples) { this.audioSamples = audioSamples; }
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}