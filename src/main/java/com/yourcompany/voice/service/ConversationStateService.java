package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * Conversation State Service for managing follow-up questions and dialogue context
 * Enables natural conversation flow with context awareness and follow-up capabilities
 */
@Service
public class ConversationStateService {
    
    // Active conversation states by session
    private final Map<String, ConversationState> sessionStates = new ConcurrentHashMap<>();
    
    // Question templates for different contexts
    private final Map<String, List<String>> followUpTemplates = new HashMap<>();
    
    public ConversationStateService() {
        initializeFollowUpTemplates();
    }
    
    /**
     * Initialize follow-up question templates
     */
    private void initializeFollowUpTemplates() {
        // RAG search follow-ups
        followUpTemplates.put("RAG_SEARCH", Arrays.asList(
            "Would you like me to explain any of these points in more detail?",
            "Should I search for more specific information about any of these topics?",
            "Would you like me to show you related documentation?",
            "Is there a particular aspect you'd like to explore further?"
        ));
        
        // Navigation follow-ups (varied responses)
        followUpTemplates.put("NAVIGATION", Arrays.asList(
            "Is this what you were looking for?",
            "Would you like me to help you find something specific here?",
            "Should I explain what you can do on this page?",
            "Do you need help with any of the options here?",
            "What would you like to do next?",
            "Is there something specific you're trying to find?",
            "How can I help you with this page?",
            "Would you like me to walk you through the available options?",
            "Let me know if you need help with anything here.",
            "What are you looking to accomplish?"
        ));
        
        // Error follow-ups
        followUpTemplates.put("ERROR", Arrays.asList(
            "Let me try a different approach. What specifically are you trying to do?",
            "I can help you troubleshoot this. Can you tell me more about what you expected?",
            "Would you like me to suggest some alternatives?",
            "Should I guide you through this step by step?"
        ));
        
        // General follow-ups
        followUpTemplates.put("GENERAL", Arrays.asList(
            "Is there anything else I can help you with?",
            "Would you like me to walk you through the available options?",
            "Should I provide more details about any of this?",
            "What would you like to do next?"
        ));
        
        // Confirmation follow-ups
        followUpTemplates.put("CONFIRMATION", Arrays.asList(
            "Did that work as expected?",
            "Is this what you were looking for?",
            "Should I continue with the next step?",
            "Would you like me to make any adjustments?"
        ));
    }
    
    /**
     * Set conversation context for follow-up questions
     */
    public void setConversationContext(String sessionId, String context, Map<String, Object> contextData) {
        ConversationState state = getOrCreateState(sessionId);
        state.lastContext = context;
        state.contextData = new HashMap<>(contextData);
        state.lastInteraction = LocalDateTime.now();
        state.expectingFollowUp = true;
    }
    
    /**
     * Add potential follow-up questions based on context
     */
    public void addPotentialFollowUps(String sessionId, String contextType, List<String> customQuestions) {
        ConversationState state = getOrCreateState(sessionId);
        state.potentialFollowUps.clear();
        
        // Add custom questions first
        if (customQuestions != null && !customQuestions.isEmpty()) {
            state.potentialFollowUps.addAll(customQuestions);
        }
        
        // Add template questions, avoiding recently used ones
        List<String> templates = followUpTemplates.get(contextType);
        if (templates != null) {
            List<String> availableTemplates = filterRecentlyUsed(state, templates);
            state.potentialFollowUps.addAll(availableTemplates);
        }
        
        // Add general questions if we don't have enough
        if (state.potentialFollowUps.size() < 2) {
            List<String> generalTemplates = followUpTemplates.get("GENERAL");
            List<String> availableGeneral = filterRecentlyUsed(state, generalTemplates);
            state.potentialFollowUps.addAll(availableGeneral);
        }
    }
    
    /**
     * Filter out recently used follow-up questions to avoid repetition
     */
    private List<String> filterRecentlyUsed(ConversationState state, List<String> templates) {
        List<String> available = new ArrayList<>();
        Set<String> recentQuestions = getRecentFollowUpQuestions(state);
        
        for (String template : templates) {
            // Only add if not used in recent interactions
            if (!recentQuestions.contains(template)) {
                available.add(template);
            }
        }
        
        // If all questions were recently used, use them anyway but shuffle
        if (available.isEmpty()) {
            available.addAll(templates);
            Collections.shuffle(available);
        }
        
        return available;
    }
    
    /**
     * Get follow-up questions used in recent conversation history
     */
    private Set<String> getRecentFollowUpQuestions(ConversationState state) {
        Set<String> recentQuestions = new HashSet<>();
        
        // Look at last 5 interactions for recently used follow-ups
        int startIndex = Math.max(0, state.conversationHistory.size() - 5);
        for (int i = startIndex; i < state.conversationHistory.size(); i++) {
            String interaction = state.conversationHistory.get(i);
            // Extract questions from interaction history
            if (interaction.contains("?")) {
                String[] parts = interaction.split("\\?");
                for (String part : parts) {
                    if (part.trim().length() > 10) { // Only consider substantial questions
                        recentQuestions.add(part.trim() + "?");
                    }
                }
            }
        }
        
        return recentQuestions;
    }
    
    /**
     * Get an appropriate follow-up question
     */
    public String getFollowUpQuestion(String sessionId) {
        ConversationState state = sessionStates.get(sessionId);
        if (state == null || state.potentialFollowUps.isEmpty()) {
            return null;
        }
        
        // Select question based on conversation patterns
        String question = selectBestFollowUp(state);
        state.expectingFollowUp = false;
        return question;
    }
    
    /**
     * Check if the input is likely a follow-up to previous context
     */
    public boolean isFollowUpInput(String sessionId, String input) {
        ConversationState state = sessionStates.get(sessionId);
        if (state == null || !state.expectingFollowUp) {
            return false;
        }
        
        // Check for follow-up indicators
        String lower = input.toLowerCase().trim();
        return lower.startsWith("yes") || lower.startsWith("no") || 
               lower.contains("more") || lower.contains("explain") ||
               lower.contains("tell me") || lower.contains("show me") ||
               lower.contains("that") || lower.contains("it") ||
               lower.length() < 10; // Short responses are often follow-ups
    }
    
    /**
     * Process follow-up input with context awareness
     */
    public FollowUpResponse processFollowUp(String sessionId, String input) {
        ConversationState state = sessionStates.get(sessionId);
        if (state == null) {
            return null;
        }
        
        String lower = input.toLowerCase().trim();
        FollowUpResponse response = new FollowUpResponse();
        response.isFollowUp = true;
        response.originalContext = state.lastContext;
        response.contextData = new HashMap<>(state.contextData);
        
        // Analyze follow-up intent
        if (isPositiveResponse(lower)) {
            response.intent = "CONTINUE";
            response.suggestion = "Let me provide more details about " + state.lastContext;
        } else if (isNegativeResponse(lower)) {
            response.intent = "STOP";
            response.suggestion = "Understood. What else can I help you with?";
        } else if (containsSpecificRequest(lower)) {
            response.intent = "SPECIFIC";
            response.suggestion = "I'll help you with that specific aspect.";
            response.specificRequest = extractSpecificRequest(lower, state.contextData);
        } else {
            response.intent = "CLARIFY";
            response.suggestion = "Could you be more specific about what you'd like to know?";
        }
        
        return response;
    }
    
    /**
     * Clear conversation state for a session
     */
    public void clearConversationState(String sessionId) {
        sessionStates.remove(sessionId);
    }
    
    /**
     * Get conversation history for context
     */
    public List<String> getConversationHistory(String sessionId, int limit) {
        ConversationState state = sessionStates.get(sessionId);
        if (state == null) {
            return new ArrayList<>();
        }
        
        List<String> history = state.conversationHistory;
        if (history.size() <= limit) {
            return new ArrayList<>(history);
        }
        
        return new ArrayList<>(history.subList(history.size() - limit, history.size()));
    }
    
    /**
     * Add to conversation history
     */
    public void addToConversationHistory(String sessionId, String interaction) {
        ConversationState state = getOrCreateState(sessionId);
        state.conversationHistory.add(interaction);
        
        // Keep only recent history
        if (state.conversationHistory.size() > 20) {
            state.conversationHistory.remove(0);
        }
    }
    
    /**
     * Generate context-aware response with follow-up capabilities
     */
    public String generateContextualResponse(String sessionId, String baseResponse, String contextType) {
        ConversationState state = getOrCreateState(sessionId);
        
        // Only add follow-up 60% of the time to avoid repetition
        if (Math.random() < 0.6) {
            String followUp = getFollowUpQuestion(sessionId);
            if (followUp != null && !baseResponse.endsWith("?")) {
                return baseResponse + " " + followUp;
            }
        }
        
        return baseResponse;
    }
    
    // Helper methods
    
    private ConversationState getOrCreateState(String sessionId) {
        return sessionStates.computeIfAbsent(sessionId, k -> new ConversationState());
    }
    
    private String selectBestFollowUp(ConversationState state) {
        if (state.potentialFollowUps.isEmpty()) {
            List<String> generalQuestions = followUpTemplates.get("GENERAL");
            return generalQuestions.get(new Random().nextInt(generalQuestions.size()));
        }
        
        // Randomly select from available follow-ups to avoid repetition
        Random random = new Random();
        return state.potentialFollowUps.get(random.nextInt(state.potentialFollowUps.size()));
    }
    
    private boolean isPositiveResponse(String input) {
        return input.startsWith("yes") || input.startsWith("yeah") || 
               input.startsWith("sure") || input.startsWith("okay") ||
               input.contains("please") || input.contains("more");
    }
    
    private boolean isNegativeResponse(String input) {
        return input.startsWith("no") || input.startsWith("nah") ||
               input.startsWith("stop") || input.contains("enough") ||
               input.contains("that's all");
    }
    
    private boolean containsSpecificRequest(String input) {
        return input.contains("about") || input.contains("explain") ||
               input.contains("tell me") || input.contains("show me") ||
               input.contains("how") || input.contains("what") ||
               input.contains("when") || input.contains("where");
    }
    
    private String extractSpecificRequest(String input, Map<String, Object> contextData) {
        // Extract the specific topic they're asking about
        String[] words = input.split(" ");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("about") || words[i].equals("explain")) {
                return String.join(" ", Arrays.copyOfRange(words, i + 1, words.length));
            }
        }
        return input;
    }
    
    // Inner classes
    
    private static class ConversationState {
        String lastContext;
        Map<String, Object> contextData = new HashMap<>();
        List<String> potentialFollowUps = new ArrayList<>();
        List<String> conversationHistory = new ArrayList<>();
        LocalDateTime lastInteraction;
        boolean expectingFollowUp = false;
    }
    
    public static class FollowUpResponse {
        public boolean isFollowUp;
        public String intent; // CONTINUE, STOP, SPECIFIC, CLARIFY
        public String suggestion;
        public String originalContext;
        public Map<String, Object> contextData;
        public String specificRequest;
    }
} 