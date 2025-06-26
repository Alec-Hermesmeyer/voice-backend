package com.yourcompany.voice.exception;

import java.util.Map;
import java.util.HashMap;

/**
 * Provides context information for error handling decisions
 */
public class ErrorContext {
    
    private final String operation;
    private final String userInput;
    private final String currentPage;
    private final String speakerId;
    private final boolean userInitiated;
    private final boolean criticalOperation;
    private final Map<String, Object> additionalContext;
    
    private ErrorContext(Builder builder) {
        this.operation = builder.operation;
        this.userInput = builder.userInput;
        this.currentPage = builder.currentPage;
        this.speakerId = builder.speakerId;
        this.userInitiated = builder.userInitiated;
        this.criticalOperation = builder.criticalOperation;
        this.additionalContext = builder.additionalContext;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getOperation() { return operation; }
    public String getUserInput() { return userInput; }
    public String getCurrentPage() { return currentPage; }
    public String getSpeakerId() { return speakerId; }
    public boolean isUserInitiated() { return userInitiated; }
    public boolean isCriticalOperation() { return criticalOperation; }
    public Map<String, Object> getAdditionalContext() { return additionalContext; }
    
    @Override
    public String toString() {
        return String.format(
            "ErrorContext{operation='%s', userInput='%s', currentPage='%s', speakerId='%s', userInitiated=%s, critical=%s}",
            operation, userInput, currentPage, speakerId, userInitiated, criticalOperation
        );
    }
    
    public static class Builder {
        private String operation;
        private String userInput;
        private String currentPage;
        private String speakerId;
        private boolean userInitiated = false;
        private boolean criticalOperation = false;
        private Map<String, Object> additionalContext = new HashMap<>();
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder userInput(String userInput) {
            this.userInput = userInput;
            return this;
        }
        
        public Builder currentPage(String currentPage) {
            this.currentPage = currentPage;
            return this;
        }
        
        public Builder speakerId(String speakerId) {
            this.speakerId = speakerId;
            return this;
        }
        
        public Builder userInitiated(boolean userInitiated) {
            this.userInitiated = userInitiated;
            return this;
        }
        
        public Builder criticalOperation(boolean criticalOperation) {
            this.criticalOperation = criticalOperation;
            return this;
        }
        
        public Builder addContext(String key, Object value) {
            this.additionalContext.put(key, value);
            return this;
        }
        
        public ErrorContext build() {
            return new ErrorContext(this);
        }
    }
}

/**
 * Error response strategies
 */
enum ErrorResponseStrategy {
    ENCOURAGE_RETRY,        // "Please try again"
    EXPLAIN_AND_SUGGEST,    // Explain what went wrong and suggest alternatives
    EXPLAIN_AND_GUIDE,      // Provide specific guidance
    FALLBACK_TO_TEXT,       // Use text instead of voice
    FALLBACK_TO_GENERAL,    // Use general knowledge instead of RAG
    CONTINUE_GRACEFULLY,    // Continue without the failed feature
    RETRY_WITH_FALLBACK,    // Retry with fallback option
    ESCALATE,              // Try alternative approach
    TERMINATE              // End session
}

/**
 * Recovery actions that can be suggested
 */
enum RecoveryAction {
    RETRY,                     // Simply try again
    RETRY_AUDIO,              // Try speaking again
    RESTART_SESSION,          // Start a new session
    USE_TEXT_MODE,            // Switch to text-only mode
    CONTINUE_WITHOUT_SPEAKER_ID,  // Continue without speaker identification
    SHOW_HELP,                // Show available commands/help
    WAIT_AND_RETRY,           // Wait a moment then retry
    CONTACT_SUPPORT           // Contact technical support
}

/**
 * Session error state tracking
 */
class SessionErrorState {
    private final String sessionId;
    private final Map<VoiceErrorCode, Integer> errorCounts = new HashMap<>();
    private final Map<VoiceErrorCode, Long> lastErrorTimes = new HashMap<>();
    private int totalErrors = 0;
    private long sessionStartTime;
    
    public SessionErrorState(String sessionId) {
        this.sessionId = sessionId;
        this.sessionStartTime = System.currentTimeMillis();
    }
    
    public void addError(VoiceException exception) {
        VoiceErrorCode errorCode = exception.getErrorCode();
        errorCounts.merge(errorCode, 1, Integer::sum);
        lastErrorTimes.put(errorCode, System.currentTimeMillis());
        totalErrors++;
    }
    
    public int getConsecutiveErrors(VoiceErrorCode errorCode) {
        return errorCounts.getOrDefault(errorCode, 0);
    }
    
    public long getTimeSinceLastError(VoiceErrorCode errorCode) {
        Long lastTime = lastErrorTimes.get(errorCode);
        return lastTime != null ? System.currentTimeMillis() - lastTime : Long.MAX_VALUE;
    }
    
    public int getTotalErrors() {
        return totalErrors;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public long getSessionStartTime() {
        return sessionStartTime;
    }
}

/**
 * Voice error response object
 */
class VoiceErrorResponse {
    private final boolean success;
    private final VoiceErrorCode errorCode;
    private final String userMessage;
    private final String technicalMessage;
    private final boolean recoverable;
    private final ErrorResponseStrategy strategy;
    private final RecoveryAction recoveryAction;
    private final boolean retryRecommended;
    
    private VoiceErrorResponse(Builder builder) {
        this.success = builder.success;
        this.errorCode = builder.errorCode;
        this.userMessage = builder.userMessage;
        this.technicalMessage = builder.technicalMessage;
        this.recoverable = builder.recoverable;
        this.strategy = builder.strategy;
        this.recoveryAction = builder.recoveryAction;
        this.retryRecommended = builder.retryRecommended;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public VoiceErrorCode getErrorCode() { return errorCode; }
    public String getUserMessage() { return userMessage; }
    public String getTechnicalMessage() { return technicalMessage; }
    public boolean isRecoverable() { return recoverable; }
    public ErrorResponseStrategy getStrategy() { return strategy; }
    public RecoveryAction getRecoveryAction() { return recoveryAction; }
    public boolean isRetryRecommended() { return retryRecommended; }
    
    public static class Builder {
        private boolean success;
        private VoiceErrorCode errorCode;
        private String userMessage;
        private String technicalMessage;
        private boolean recoverable;
        private ErrorResponseStrategy strategy;
        private RecoveryAction recoveryAction;
        private boolean retryRecommended;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorCode(VoiceErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }
        
        public Builder technicalMessage(String technicalMessage) {
            this.technicalMessage = technicalMessage;
            return this;
        }
        
        public Builder recoverable(boolean recoverable) {
            this.recoverable = recoverable;
            return this;
        }
        
        public Builder strategy(ErrorResponseStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public Builder recoveryAction(RecoveryAction recoveryAction) {
            this.recoveryAction = recoveryAction;
            return this;
        }
        
        public Builder retryRecommended(boolean retryRecommended) {
            this.retryRecommended = retryRecommended;
            return this;
        }
        
        public VoiceErrorResponse build() {
            return new VoiceErrorResponse(this);
        }
    }
}