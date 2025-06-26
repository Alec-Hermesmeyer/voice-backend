package com.yourcompany.voice.exception;

import com.yourcompany.voice.service.VoiceResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Centralized error handling for the voice system
 * Provides consistent error responses and recovery strategies
 */
@Service
public class VoiceErrorHandler {
    
    private static final Logger logger = Logger.getLogger(VoiceErrorHandler.class.getName());
    
    @Autowired
    private VoiceResponseService responseService;
    
    // Track error patterns per session
    private final Map<String, SessionErrorState> sessionErrorStates = new ConcurrentHashMap<>();
    
    /**
     * Handle voice exception with context
     */
    public VoiceErrorResponse handleError(String sessionId, VoiceException exception, ErrorContext context) {
        try {
            // Log the error
            logError(sessionId, exception, context);
            
            // Track error patterns
            SessionErrorState errorState = trackError(sessionId, exception);
            
            // Determine appropriate response strategy
            ErrorResponseStrategy strategy = determineStrategy(exception, errorState, context);
            
            // Generate user-friendly response
            String userMessage = generateUserMessage(exception, strategy, errorState, context);
            
            // Send response to user
            sendErrorResponse(sessionId, userMessage, exception, strategy);
            
            // Suggest recovery actions
            RecoveryAction recoveryAction = suggestRecovery(exception, strategy, context);
            
            return VoiceErrorResponse.builder()
                .success(false)
                .errorCode(exception.getErrorCode())
                .userMessage(userMessage)
                .technicalMessage(exception.getMessage())
                .recoverable(exception.isRecoverable())
                .strategy(strategy)
                .recoveryAction(recoveryAction)
                .retryRecommended(shouldRetry(exception, errorState))
                .build();
            
        } catch (Exception handlingError) {
            logger.severe("Error in error handler: " + handlingError.getMessage());
            return createFallbackResponse(sessionId, exception);
        }
    }
    
    /**
     * Handle runtime exceptions
     */
    public VoiceErrorResponse handleRuntimeError(String sessionId, VoiceRuntimeException exception, ErrorContext context) {
        VoiceException voiceException = new VoiceException(
            exception.getErrorCode(), 
            exception.getMessage(), 
            exception.getUserFriendlyMessage(),
            exception.getCause()
        );
        return handleError(sessionId, voiceException, context);
    }
    
    /**
     * Handle unexpected exceptions
     */
    public VoiceErrorResponse handleUnexpectedError(String sessionId, Exception exception, ErrorContext context) {
        VoiceException voiceException = new VoiceException(
            VoiceErrorCode.INTERNAL_ERROR,
            "Unexpected error: " + exception.getMessage(),
            VoiceErrorCode.INTERNAL_ERROR.getUserFriendlyMessage(),
            exception
        );
        return handleError(sessionId, voiceException, context);
    }
    
    private void logError(String sessionId, VoiceException exception, ErrorContext context) {
        String logMessage = String.format(
            "Voice Error [%s] in session [%s]: %s | Context: %s | User-friendly: %s",
            exception.getErrorCode().getCode(),
            sessionId,
            exception.getMessage(),
            context != null ? context.toString() : "none",
            exception.getUserFriendlyMessage()
        );
        
        if (exception.isRecoverable()) {
            logger.warning(logMessage);
        } else {
            logger.severe(logMessage);
        }
        
        // Log stack trace for non-recoverable errors
        if (!exception.isRecoverable() && exception.getCause() != null) {
            logger.severe("Stack trace: " + java.util.Arrays.toString(exception.getStackTrace()));
        }
    }
    
    private SessionErrorState trackError(String sessionId, VoiceException exception) {
        SessionErrorState errorState = sessionErrorStates.computeIfAbsent(
            sessionId, 
            id -> new SessionErrorState(id)
        );
        
        errorState.addError(exception);
        return errorState;
    }
    
    private ErrorResponseStrategy determineStrategy(VoiceException exception, 
                                                  SessionErrorState errorState, 
                                                  ErrorContext context) {
        
        VoiceErrorCode errorCode = exception.getErrorCode();
        
        // Check for repeated errors
        if (errorState.getConsecutiveErrors(errorCode) >= 3) {
            return ErrorResponseStrategy.ESCALATE;
        }
        
        // Check error severity
        if (!exception.isRecoverable()) {
            return ErrorResponseStrategy.TERMINATE;
        }
        
        // Context-based strategies
        if (context != null) {
            if (context.isCriticalOperation()) {
                return ErrorResponseStrategy.RETRY_WITH_FALLBACK;
            }
            
            if (context.isUserInitiated()) {
                return ErrorResponseStrategy.EXPLAIN_AND_SUGGEST;
            }
        }
        
        // Default strategies by error type
        switch (errorCode) {
            case STT_FAILED:
            case AUDIO_INVALID:
                return ErrorResponseStrategy.ENCOURAGE_RETRY;
                
            case TTS_FAILED:
                return ErrorResponseStrategy.FALLBACK_TO_TEXT;
                
            case SPEAKER_ID_FAILED:
                return ErrorResponseStrategy.CONTINUE_GRACEFULLY;
                
            case TURN_NOT_ALLOWED:
                return ErrorResponseStrategy.EXPLAIN_AND_GUIDE;
                
            case RAG_SEARCH_FAILED:
                return ErrorResponseStrategy.FALLBACK_TO_GENERAL;
                
            default:
                return ErrorResponseStrategy.EXPLAIN_AND_SUGGEST;
        }
    }
    
    private String generateUserMessage(VoiceException exception, 
                                     ErrorResponseStrategy strategy, 
                                     SessionErrorState errorState,
                                     ErrorContext context) {
        
        String baseMessage = exception.getUserFriendlyMessage();
        
        // Modify message based on strategy
        switch (strategy) {
            case ENCOURAGE_RETRY:
                if (errorState.getConsecutiveErrors(exception.getErrorCode()) > 1) {
                    return baseMessage + " Take your time and try speaking clearly.";
                }
                return baseMessage;
                
            case EXPLAIN_AND_GUIDE:
                return baseMessage + " " + getGuidanceMessage(exception.getErrorCode(), context);
                
            case FALLBACK_TO_TEXT:
                return "I can't speak right now, but I can still help you through text responses.";
                
            case CONTINUE_GRACEFULLY:
                return baseMessage + " I'll continue helping you anyway.";
                
            case ESCALATE:
                return "I'm having persistent issues. Let me try a different approach.";
                
            case TERMINATE:
                return baseMessage + " Please start a new session.";
                
            default:
                return baseMessage;
        }
    }
    
    private String getGuidanceMessage(VoiceErrorCode errorCode, ErrorContext context) {
        switch (errorCode) {
            case TURN_NOT_ALLOWED:
                return "Please wait for the current speaker to finish before speaking.";
                
            case COMMAND_NOT_RECOGNIZED:
                return "Try using simpler commands like 'go to dashboard' or 'help me navigate'.";
                
            case RAG_NO_RESULTS:
                return "Try asking about a different topic or being more specific.";
                
            case AUDIO_TOO_SHORT:
                return "Please speak for at least a few seconds so I can understand you better.";
                
            case STT_NO_SPEECH:
                return "Make sure your microphone is working and speak clearly.";
                
            default:
                return "Please try again in a moment.";
        }
    }
    
    private void sendErrorResponse(String sessionId, String userMessage, 
                                  VoiceException exception, ErrorResponseStrategy strategy) {
        
        switch (strategy) {
            case FALLBACK_TO_TEXT:
                responseService.sendTextResponse(sessionId, userMessage);
                break;
                
            case TERMINATE:
                responseService.sendErrorResponse(sessionId, userMessage);
                responseService.sendSessionStatus(sessionId, "terminated", exception.getErrorCode().getCode());
                break;
                
            default:
                responseService.sendErrorResponse(sessionId, userMessage);
                break;
        }
    }
    
    private RecoveryAction suggestRecovery(VoiceException exception, 
                                         ErrorResponseStrategy strategy, 
                                         ErrorContext context) {
        
        switch (exception.getErrorCode()) {
            case SESSION_EXPIRED:
                return RecoveryAction.RESTART_SESSION;
                
            case STT_FAILED:
            case AUDIO_INVALID:
                return RecoveryAction.RETRY_AUDIO;
                
            case TTS_FAILED:
                return RecoveryAction.USE_TEXT_MODE;
                
            case SPEAKER_ID_FAILED:
                return RecoveryAction.CONTINUE_WITHOUT_SPEAKER_ID;
                
            case COMMAND_NOT_RECOGNIZED:
                return RecoveryAction.SHOW_HELP;
                
            case API_RATE_LIMITED:
                return RecoveryAction.WAIT_AND_RETRY;
                
            default:
                return exception.isRecoverable() ? RecoveryAction.RETRY : RecoveryAction.RESTART_SESSION;
        }
    }
    
    private boolean shouldRetry(VoiceException exception, SessionErrorState errorState) {
        // Don't retry if not recoverable
        if (!exception.isRecoverable()) {
            return false;
        }
        
        // Don't retry if too many consecutive errors
        if (errorState.getConsecutiveErrors(exception.getErrorCode()) >= 3) {
            return false;
        }
        
        // Don't retry certain error types immediately
        switch (exception.getErrorCode()) {
            case API_RATE_LIMITED:
            case API_QUOTA_EXCEEDED:
                return false;
            default:
                return true;
        }
    }
    
    private VoiceErrorResponse createFallbackResponse(String sessionId, VoiceException exception) {
        String fallbackMessage = "I'm experiencing technical difficulties. Please try again.";
        responseService.sendErrorResponse(sessionId, fallbackMessage);
        
        return VoiceErrorResponse.builder()
            .success(false)
            .errorCode(VoiceErrorCode.INTERNAL_ERROR)
            .userMessage(fallbackMessage)
            .technicalMessage("Error handler failure")
            .recoverable(true)
            .strategy(ErrorResponseStrategy.ENCOURAGE_RETRY)
            .recoveryAction(RecoveryAction.RETRY)
            .retryRecommended(true)
            .build();
    }
    
    /**
     * Clear error state for session (when session ends)
     */
    public void clearSessionErrors(String sessionId) {
        sessionErrorStates.remove(sessionId);
    }
    
    /**
     * Get error statistics for monitoring
     */
    public Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        int totalSessions = sessionErrorStates.size();
        int sessionsWithErrors = 0;
        int totalErrors = 0;
        
        for (SessionErrorState state : sessionErrorStates.values()) {
            if (state.getTotalErrors() > 0) {
                sessionsWithErrors++;
                totalErrors += state.getTotalErrors();
            }
        }
        
        stats.put("totalSessionsTracked", totalSessions);
        stats.put("sessionsWithErrors", sessionsWithErrors);
        stats.put("totalErrors", totalErrors);
        stats.put("averageErrorsPerSession", totalSessions > 0 ? (double) totalErrors / totalSessions : 0);
        
        return stats;
    }
}