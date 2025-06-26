package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error information DTO
 */
public class ErrorInfoDTO {
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("userMessage")
    private String userMessage;
    
    @JsonProperty("recoverable")
    private boolean recoverable;
    
    @JsonProperty("retryRecommended")
    private boolean retryRecommended;
    
    @JsonProperty("suggestedAction")
    private String suggestedAction;
    
    // Constructors
    public ErrorInfoDTO() {}
    
    public ErrorInfoDTO(String errorCode, String message, String userMessage, boolean recoverable) {
        this.errorCode = errorCode;
        this.message = message;
        this.userMessage = userMessage;
        this.recoverable = recoverable;
    }
    
    // Getters and setters
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    public boolean isRecoverable() { return recoverable; }
    public void setRecoverable(boolean recoverable) { this.recoverable = recoverable; }
    public boolean isRetryRecommended() { return retryRecommended; }
    public void setRetryRecommended(boolean retryRecommended) { this.retryRecommended = retryRecommended; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
}