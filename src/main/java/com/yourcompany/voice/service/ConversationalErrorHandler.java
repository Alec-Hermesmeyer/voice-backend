package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversational Error Handler for user-friendly error recovery
 * Provides helpful, conversational responses to errors with actionable suggestions
 */
@Service
public class ConversationalErrorHandler {
    
    // Error patterns and their conversational responses
    private final Map<String, ErrorPattern> errorPatterns = new HashMap<>();
    
    // Recent error history to avoid repetition
    private final Map<String, List<String>> sessionErrorHistory = new ConcurrentHashMap<>();
    
    public ConversationalErrorHandler() {
        initializeErrorPatterns();
    }
    
    /**
     * Initialize common error patterns and responses
     */
    private void initializeErrorPatterns() {
        // Navigation errors
        errorPatterns.put("PAGE_NOT_FOUND", new ErrorPattern(
            "I couldn't find that page.",
            Arrays.asList(
                "Let me help you find what you're looking for. Could you describe what you were trying to access?",
                "I can show you the available pages. Would you like me to list them?",
                "Perhaps you meant something similar? Try saying the page name differently."
            ),
            Arrays.asList("show available pages", "list navigation options", "help me navigate")
        ));
        
        // Command recognition errors
        errorPatterns.put("COMMAND_NOT_RECOGNIZED", new ErrorPattern(
            "I didn't quite understand that command.",
            Arrays.asList(
                "Could you try rephrasing that? I'm here to help with navigation and information.",
                "I can help you with navigation, searching, and form filling. What would you like to do?",
                "Let me know what you're trying to accomplish, and I'll guide you through it."
            ),
            Arrays.asList("show available commands", "what can you do", "help me")
        ));
        
        // Search errors
        errorPatterns.put("NO_SEARCH_RESULTS", new ErrorPattern(
            "I couldn't find any information about that.",
            Arrays.asList(
                "Let me try searching with different terms. Can you describe what you're looking for differently?",
                "Would you like me to search in a different section or database?",
                "I can help you refine your search. What specific aspect are you most interested in?"
            ),
            Arrays.asList("search differently", "try another approach", "refine search")
        ));
        
        // Form filling errors
        errorPatterns.put("FORM_FIELD_ERROR", new ErrorPattern(
            "I had trouble filling out that field.",
            Arrays.asList(
                "Let me help you with this form field. What information should I enter?",
                "Could you spell that out or provide it in a different format?",
                "I can guide you through each field step by step. Should I do that?"
            ),
            Arrays.asList("help with form", "guide me through this", "try again")
        ));
        
        // UI interaction errors
        errorPatterns.put("ELEMENT_NOT_FOUND", new ErrorPattern(
            "I couldn't find that button or element on the page.",
            Arrays.asList(
                "Let me check what's available on this page. Can you describe what you're trying to click?",
                "The page might have changed. Should I refresh and try again?",
                "I can read out the available options on this page. Would that help?"
            ),
            Arrays.asList("show page options", "refresh page", "try different approach")
        ));
        
        // Voice recognition errors
        errorPatterns.put("SPEECH_NOT_CLEAR", new ErrorPattern(
            "I had trouble understanding what you said.",
            Arrays.asList(
                "Could you repeat that a bit more slowly?",
                "I might have missed that. Could you try saying it differently?",
                "If you're in a noisy environment, try speaking a bit louder and clearer."
            ),
            Arrays.asList("repeat slowly", "try again", "speak clearly")
        ));
        
        // System errors
        errorPatterns.put("SYSTEM_ERROR", new ErrorPattern(
            "I encountered a technical issue.",
            Arrays.asList(
                "Let me try that again in a moment. Technical issues usually resolve quickly.",
                "I can try a different approach to accomplish what you wanted. Should I do that?",
                "If this keeps happening, you might want to refresh the page. I can guide you through that."
            ),
            Arrays.asList("try again", "use different approach", "refresh page")
        ));
        
        // Timeout errors
        errorPatterns.put("TIMEOUT", new ErrorPattern(
            "That took longer than expected.",
            Arrays.asList(
                "The system might be busy. Should I try again?",
                "Let me try a simpler approach that might work faster.",
                "Would you like me to check the system status first?"
            ),
            Arrays.asList("try again", "check status", "simpler approach")
        ));
    }
    
    /**
     * Handle an error with conversational response
     */
    public ErrorResponse handleError(String sessionId, String errorType, String originalInput, 
                                   Map<String, Object> errorContext) {
        ErrorPattern pattern = errorPatterns.get(errorType);
        if (pattern == null) {
            pattern = errorPatterns.get("SYSTEM_ERROR"); // Default fallback
        }
        
        // Track error history
        trackErrorHistory(sessionId, errorType);
        
        // Generate appropriate response
        String response = generateErrorResponse(sessionId, pattern, originalInput, errorContext);
        
        // Create error response object
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.success = false;
        errorResponse.errorType = errorType;
        errorResponse.userFriendlyMessage = response;
        errorResponse.originalInput = originalInput;
        errorResponse.suggestions = new ArrayList<>(pattern.suggestions);
        errorResponse.recoveryOptions = generateRecoveryOptions(pattern, errorContext);
        errorResponse.conversational = true;
        
        return errorResponse;
    }
    
    /**
     * Generate contextual error response
     */
    private String generateErrorResponse(String sessionId, ErrorPattern pattern, 
                                       String originalInput, Map<String, Object> errorContext) {
        StringBuilder response = new StringBuilder();
        
        // Start with the base message
        response.append(pattern.baseMessage);
        
        // Add specific context if available
        if (originalInput != null && !originalInput.trim().isEmpty()) {
            response.append(" I heard you say \"").append(originalInput).append("\".");
        }
        
        // Add appropriate follow-up based on error history
        List<String> errorHistory = sessionErrorHistory.get(sessionId);
        if (errorHistory != null && errorHistory.size() > 1) {
            // User has had multiple errors
            response.append(" I notice you're having some trouble. ");
            response.append("Would you like me to walk you through this step by step?");
        } else {
            // First time error, use standard follow-up
            response.append(" ").append(selectFollowUpMessage(pattern));
        }
        
        return response.toString();
    }
    
    /**
     * Generate recovery options based on error context
     */
    private List<String> generateRecoveryOptions(ErrorPattern pattern, Map<String, Object> errorContext) {
        List<String> options = new ArrayList<>(pattern.suggestions);
        
        // Add context-specific options
        if (errorContext != null) {
            String currentPage = (String) errorContext.get("page");
            if (currentPage != null) {
                options.add("go back to previous page");
                options.add("start over from " + currentPage);
            }
            
            Boolean canRetry = (Boolean) errorContext.get("canRetry");
            if (canRetry != null && canRetry) {
                options.add("try the same action again");
            }
        }
        
        // Always add general help option
        options.add("get general help");
        
        return options;
    }
    
    /**
     * Track error history to improve responses
     */
    private void trackErrorHistory(String sessionId, String errorType) {
        sessionErrorHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(errorType);
        
        // Keep only recent errors (last 10)
        List<String> history = sessionErrorHistory.get(sessionId);
        if (history.size() > 10) {
            history.remove(0);
        }
    }
    
    /**
     * Select appropriate follow-up message
     */
    private String selectFollowUpMessage(ErrorPattern pattern) {
        if (pattern.followUps.isEmpty()) {
            return "How would you like to proceed?";
        }
        
        // Simple random selection - can be enhanced with context awareness
        Random random = new Random();
        return pattern.followUps.get(random.nextInt(pattern.followUps.size()));
    }
    
    /**
     * Check if user is experiencing repeated errors
     */
    public boolean isExperiencingRepeatedErrors(String sessionId) {
        List<String> history = sessionErrorHistory.get(sessionId);
        return history != null && history.size() >= 3;
    }
    
    /**
     * Generate encouraging message for users with repeated errors
     */
    public String generateEncouragingMessage(String sessionId) {
        return "I know this can be frustrating. Let me take a different approach to help you. " +
               "What's the main thing you're trying to accomplish? I'll guide you through it step by step.";
    }
    
    /**
     * Clear error history for a session
     */
    public void clearErrorHistory(String sessionId) {
        sessionErrorHistory.remove(sessionId);
    }
    
    /**
     * Get error statistics for analysis
     */
    public Map<String, Integer> getErrorStatistics(String sessionId) {
        List<String> history = sessionErrorHistory.get(sessionId);
        if (history == null || history.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Integer> stats = new HashMap<>();
        for (String errorType : history) {
            stats.merge(errorType, 1, Integer::sum);
        }
        
        return stats;
    }
    
    // Inner classes
    
    private static class ErrorPattern {
        final String baseMessage;
        final List<String> followUps;
        final List<String> suggestions;
        
        ErrorPattern(String baseMessage, List<String> followUps, List<String> suggestions) {
            this.baseMessage = baseMessage;
            this.followUps = new ArrayList<>(followUps);
            this.suggestions = new ArrayList<>(suggestions);
        }
    }
    
    public static class ErrorResponse {
        public boolean success;
        public String errorType;
        public String userFriendlyMessage;
        public String originalInput;
        public List<String> suggestions;
        public List<String> recoveryOptions;
        public boolean conversational;
        
        public ErrorResponse() {
            this.suggestions = new ArrayList<>();
            this.recoveryOptions = new ArrayList<>();
            this.conversational = true;
        }
    }
} 