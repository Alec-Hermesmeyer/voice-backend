package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audio response DTO
 */
public class AudioResponseDTO {
    
    @JsonProperty("audioData")
    private String audioData; // Base64 encoded
    
    @JsonProperty("voiceModel")
    private String voiceModel;
    
    @JsonProperty("duration")
    private Double duration;
    
    @JsonProperty("format")
    private String format = "mp3";
    
    // Constructors
    public AudioResponseDTO() {}
    
    public AudioResponseDTO(String audioData, String voiceModel) {
        this.audioData = audioData;
        this.voiceModel = voiceModel;
    }
    
    // Getters and setters
    public String getAudioData() { return audioData; }
    public void setAudioData(String audioData) { this.audioData = audioData; }
    public String getVoiceModel() { return voiceModel; }
    public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}