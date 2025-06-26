package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response for processing voice input
 */
public class ProcessVoiceInputResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("transcript")
    private String transcript;
    
    @JsonProperty("responseText")
    private String responseText;
    
    @JsonProperty("responseType")
    private String responseType;
    
    @JsonProperty("speakerInfo")
    private SpeakerInfoDTO speakerInfo;
    
    @JsonProperty("turnInfo")
    private TurnInfoDTO turnInfo;
    
    @JsonProperty("audioResponse")
    private AudioResponseDTO audioResponse;
    
    @JsonProperty("uiCommands")
    private List<UICommandDTO> uiCommands;
    
    @JsonProperty("contextualInfo")
    private Object contextualInfo;
    
    @JsonProperty("error")
    private ErrorInfoDTO error;
    
    // Constructors
    public ProcessVoiceInputResponse() {}
    
    public static ProcessVoiceInputResponse success(String transcript, String responseText, String responseType) {
        ProcessVoiceInputResponse response = new ProcessVoiceInputResponse();
        response.setSuccess(true);
        response.setTranscript(transcript);
        response.setResponseText(responseText);
        response.setResponseType(responseType);
        return response;
    }
    
    public static ProcessVoiceInputResponse error(ErrorInfoDTO error) {
        ProcessVoiceInputResponse response = new ProcessVoiceInputResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }
    public SpeakerInfoDTO getSpeakerInfo() { return speakerInfo; }
    public void setSpeakerInfo(SpeakerInfoDTO speakerInfo) { this.speakerInfo = speakerInfo; }
    public TurnInfoDTO getTurnInfo() { return turnInfo; }
    public void setTurnInfo(TurnInfoDTO turnInfo) { this.turnInfo = turnInfo; }
    public AudioResponseDTO getAudioResponse() { return audioResponse; }
    public void setAudioResponse(AudioResponseDTO audioResponse) { this.audioResponse = audioResponse; }
    public List<UICommandDTO> getUiCommands() { return uiCommands; }
    public void setUiCommands(List<UICommandDTO> uiCommands) { this.uiCommands = uiCommands; }
    public Object getContextualInfo() { return contextualInfo; }
    public void setContextualInfo(Object contextualInfo) { this.contextualInfo = contextualInfo; }
    public ErrorInfoDTO getError() { return error; }
    public void setError(ErrorInfoDTO error) { this.error = error; }
}