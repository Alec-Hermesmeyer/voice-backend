package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Emotional Intelligence Service for mood and frustration detection
 * Analyzes user speech patterns, interaction patterns, and behavior for empathetic responses
 */
@Service
public class EmotionalIntelligenceService {
    
    // User emotional state tracking
    private final Map<String, EmotionalProfile> userEmotionalProfiles = new ConcurrentHashMap<>();
    
    // Emotional indicators and patterns
    private final Set<String> frustrationWords = Set.of(
        "damn", "dammit", "shit", "fuck", "stupid", "ridiculous", "annoying", 
        "frustrated", "annoyed", "irritated", "pissed", "angry", "mad",
        "hate", "terrible", "awful", "horrible", "broken", "useless",
        "why", "what the hell", "come on", "seriously", "again", "still"
    );
    
    private final Set<String> confusionWords = Set.of(
        "confused", "lost", "don't understand", "what", "how", "why",
        "unclear", "confusing", "complicated", "difficult", "hard",
        "i don't know", "not sure", "uncertain", "help", "stuck"
    );
    
    private final Set<String> positiveWords = Set.of(
        "great", "awesome", "perfect", "excellent", "good", "nice",
        "thanks", "thank you", "appreciate", "love", "like", "wonderful",
        "amazing", "fantastic", "brilliant", "cool", "sweet", "yes"
    );
    
    private final Set<String> urgencyWords = Set.of(
        "urgent", "quickly", "fast", "asap", "immediately", "now", "hurry",
        "emergency", "critical", "important", "deadline", "rush"
    );
    
    /**
     * Analyze user's emotional state from their input
     */
    public EmotionalState analyzeEmotionalState(String sessionId, String userInput, Map<String, Object> contextData) {
        EmotionalProfile profile = getOrCreateProfile(sessionId);
        
        EmotionalState currentState = new EmotionalState();
        currentState.timestamp = LocalDateTime.now();
        currentState.originalInput = userInput;
        
        // Analyze speech patterns
        analyzeSpeechPatterns(userInput, currentState);
        
        // Analyze interaction patterns from context
        analyzeInteractionPatterns(contextData, currentState, profile);
        
        // Calculate overall emotional score
        calculateEmotionalScores(currentState);
        
        // Update user profile
        updateEmotionalProfile(profile, currentState);
        
        // Generate empathetic response adjustments
        generateResponseAdjustments(currentState, profile);
        
        return currentState;
    }
    
    /**
     * Analyze speech patterns for emotional indicators
     */
    private void analyzeSpeechPatterns(String input, EmotionalState state) {
        String lowerInput = input.toLowerCase();
        
        // Count emotional indicators
        for (String word : frustrationWords) {
            if (lowerInput.contains(word)) {
                state.frustrationIndicators++;
            }
        }
        
        for (String word : confusionWords) {
            if (lowerInput.contains(word)) {
                state.confusionIndicators++;
            }
        }
        
        for (String word : positiveWords) {
            if (lowerInput.contains(word)) {
                state.positiveIndicators++;
            }
        }
        
        for (String word : urgencyWords) {
            if (lowerInput.contains(word)) {
                state.urgencyIndicators++;
            }
        }
        
        // Analyze punctuation and capitalization
        long exclamationCount = lowerInput.chars().filter(ch -> ch == '!').count();
        long questionCount = lowerInput.chars().filter(ch -> ch == '?').count();
        
        state.exclamationCount = (int) exclamationCount;
        state.questionCount = (int) questionCount;
        
        // Check for excessive capitalization (shouting)
        long upperCaseCount = input.chars().filter(Character::isUpperCase).count();
        long totalLetters = input.chars().filter(Character::isLetter).count();
        
        if (totalLetters > 0) {
            state.capsRatio = (double) upperCaseCount / totalLetters;
        }
        
        // Analyze repetition (repeated words/phrases indicating frustration)
        String[] words = lowerInput.split("\\s+");
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        if (words.length > 0) {
            state.repetitionRatio = 1.0 - ((double) uniqueWords.size() / words.length);
        }
    }
    
    /**
     * Analyze interaction patterns from context data
     */
    private void analyzeInteractionPatterns(Map<String, Object> contextData, EmotionalState state, EmotionalProfile profile) {
        // Check for repeated errors or failed attempts
        if (contextData.containsKey("errorCount")) {
            Integer errorCount = (Integer) contextData.get("errorCount");
            if (errorCount != null && errorCount > 0) {
                state.recentErrors = errorCount;
            }
        }
        
        // Check for rapid successive interactions (potential frustration)
        long timeSinceLastInteraction = ChronoUnit.SECONDS.between(profile.lastInteractionTime, LocalDateTime.now());
        if (timeSinceLastInteraction < 5) {
            state.rapidInteraction = true;
        }
        
        // Analyze session duration and interaction count
        state.sessionDuration = ChronoUnit.MINUTES.between(profile.sessionStartTime, LocalDateTime.now());
        state.interactionCount = profile.totalInteractions;
        
        // Check for page context (some pages are naturally more frustrating)
        String currentPage = (String) contextData.getOrDefault("page", "");
        if (currentPage.contains("error") || currentPage.contains("404")) {
            state.contextualFrustration = true;
        }
    }
    
    /**
     * Calculate overall emotional scores
     */
    private void calculateEmotionalScores(EmotionalState state) {
        // Frustration score (0-100)
        int frustrationScore = 0;
        frustrationScore += state.frustrationIndicators * 20;
        frustrationScore += state.recentErrors * 15;
        frustrationScore += state.exclamationCount * 10;
        frustrationScore += (int) (state.capsRatio * 30);
        frustrationScore += state.rapidInteraction ? 20 : 0;
        frustrationScore += state.contextualFrustration ? 25 : 0;
        
        state.frustrationScore = Math.min(100, frustrationScore);
        
        // Confusion score (0-100)
        int confusionScore = 0;
        confusionScore += state.confusionIndicators * 25;
        confusionScore += state.questionCount * 10;
        confusionScore += (state.interactionCount > 10 && state.sessionDuration > 15) ? 20 : 0;
        
        state.confusionScore = Math.min(100, confusionScore);
        
        // Positivity score (0-100)
        int positivityScore = 0;
        positivityScore += state.positiveIndicators * 30;
        positivityScore += (state.recentErrors == 0) ? 20 : 0;
        positivityScore += (!state.rapidInteraction) ? 10 : 0;
        
        state.positivityScore = Math.min(100, positivityScore);
        
        // Urgency score (0-100)
        int urgencyScore = 0;
        urgencyScore += state.urgencyIndicators * 30;
        urgencyScore += state.rapidInteraction ? 20 : 0;
        urgencyScore += state.exclamationCount * 15;
        
        state.urgencyScore = Math.min(100, urgencyScore);
        
        // Determine primary emotional state
        determinePrimaryEmotion(state);
    }
    
    /**
     * Determine the primary emotional state
     */
    private void determinePrimaryEmotion(EmotionalState state) {
        if (state.frustrationScore > 60) {
            state.primaryEmotion = "frustrated";
            state.intensity = state.frustrationScore > 80 ? "high" : "medium";
        } else if (state.confusionScore > 50) {
            state.primaryEmotion = "confused";
            state.intensity = state.confusionScore > 75 ? "high" : "medium";
        } else if (state.urgencyScore > 50) {
            state.primaryEmotion = "urgent";
            state.intensity = state.urgencyScore > 75 ? "high" : "medium";
        } else if (state.positivityScore > 60) {
            state.primaryEmotion = "positive";
            state.intensity = "low";
        } else {
            state.primaryEmotion = "neutral";
            state.intensity = "low";
        }
    }
    
    /**
     * Generate response adjustments based on emotional state
     */
    private void generateResponseAdjustments(EmotionalState state, EmotionalProfile profile) {
        List<String> adjustments = new ArrayList<>();
        
        switch (state.primaryEmotion) {
            case "frustrated":
                if ("high".equals(state.intensity)) {
                    adjustments.add("acknowledge_frustration");
                    adjustments.add("offer_alternative");
                    adjustments.add("use_calming_tone");
                } else {
                    adjustments.add("empathetic_response");
                    adjustments.add("provide_reassurance");
                }
                break;
                
            case "confused":
                adjustments.add("provide_clarification");
                adjustments.add("offer_step_by_step");
                adjustments.add("use_simple_language");
                break;
                
            case "urgent":
                adjustments.add("acknowledge_urgency");
                adjustments.add("prioritize_efficiency");
                adjustments.add("direct_response");
                break;
                
            case "positive":
                adjustments.add("maintain_positivity");
                adjustments.add("encourage_engagement");
                break;
                
            default:
                adjustments.add("standard_response");
        }
        
        state.responseAdjustments = adjustments;
    }
    
    /**
     * Get empathetic response based on emotional state
     */
    public String getEmpatheticResponse(EmotionalState emotionalState, String baseResponse) {
        if (emotionalState == null || "neutral".equals(emotionalState.primaryEmotion)) {
            return baseResponse;
        }
        
        StringBuilder empathetic = new StringBuilder();
        
        switch (emotionalState.primaryEmotion) {
            case "frustrated":
                if ("high".equals(emotionalState.intensity)) {
                    empathetic.append("I can tell you're really frustrated. Let me try a different approach. ");
                } else {
                    empathetic.append("I understand this might be frustrating. ");
                }
                break;
                
            case "confused":
                empathetic.append("I can see this is confusing. Let me break this down step by step. ");
                break;
                
            case "urgent":
                empathetic.append("I understand this is urgent. Let me help you quickly. ");
                break;
                
            case "positive":
                empathetic.append("Great! ");
                break;
        }
        
        empathetic.append(baseResponse);
        return empathetic.toString();
    }
    
    /**
     * Check if user needs emotional support
     */
    public boolean needsEmotionalSupport(String sessionId) {
        EmotionalProfile profile = userEmotionalProfiles.get(sessionId);
        if (profile == null) {
            return false;
        }
        
        // Check recent emotional history
        long recentFrustrationEvents = profile.emotionalHistory.stream()
            .filter(state -> state.timestamp.isAfter(LocalDateTime.now().minusMinutes(10)))
            .filter(state -> "frustrated".equals(state.primaryEmotion))
            .count();
        
        return recentFrustrationEvents >= 3;
    }
    
    /**
     * Get or create emotional profile for user
     */
    private EmotionalProfile getOrCreateProfile(String sessionId) {
        return userEmotionalProfiles.computeIfAbsent(sessionId, k -> {
            EmotionalProfile profile = new EmotionalProfile();
            profile.sessionId = sessionId;
            profile.sessionStartTime = LocalDateTime.now();
            profile.lastInteractionTime = LocalDateTime.now();
            return profile;
        });
    }
    
    /**
     * Update emotional profile with new state
     */
    private void updateEmotionalProfile(EmotionalProfile profile, EmotionalState state) {
        profile.lastInteractionTime = LocalDateTime.now();
        profile.totalInteractions++;
        profile.emotionalHistory.add(state);
        
        // Keep only recent history (last 20 interactions)
        if (profile.emotionalHistory.size() > 20) {
            profile.emotionalHistory.remove(0);
        }
        
        // Update emotional trends
        updateEmotionalTrends(profile);
    }
    
    /**
     * Update emotional trends for pattern recognition
     */
    private void updateEmotionalTrends(EmotionalProfile profile) {
        if (profile.emotionalHistory.size() < 3) {
            return;
        }
        
        // Analyze recent trend (last 3 interactions)
        List<EmotionalState> recent = profile.emotionalHistory.subList(
            Math.max(0, profile.emotionalHistory.size() - 3), 
            profile.emotionalHistory.size()
        );
        
        long frustrationCount = recent.stream()
            .filter(state -> "frustrated".equals(state.primaryEmotion))
            .count();
        
        profile.recentFrustrationTrend = frustrationCount >= 2;
    }
    
    /**
     * Clear emotional profile (end of session)
     */
    public void clearEmotionalProfile(String sessionId) {
        userEmotionalProfiles.remove(sessionId);
    }
    
    // Inner classes for data structures
    
    public static class EmotionalState {
        public LocalDateTime timestamp;
        public String originalInput;
        
        // Emotional indicators
        public int frustrationIndicators = 0;
        public int confusionIndicators = 0;
        public int positiveIndicators = 0;
        public int urgencyIndicators = 0;
        
        // Speech pattern analysis
        public int exclamationCount = 0;
        public int questionCount = 0;
        public double capsRatio = 0.0;
        public double repetitionRatio = 0.0;
        
        // Interaction pattern analysis
        public int recentErrors = 0;
        public boolean rapidInteraction = false;
        public boolean contextualFrustration = false;
        public long sessionDuration = 0;
        public int interactionCount = 0;
        
        // Calculated scores
        public int frustrationScore = 0;
        public int confusionScore = 0;
        public int positivityScore = 0;
        public int urgencyScore = 0;
        
        // Primary emotional state
        public String primaryEmotion = "neutral";
        public String intensity = "low"; // low, medium, high
        
        // Response adjustments
        public List<String> responseAdjustments = new ArrayList<>();
    }
    
    public static class EmotionalProfile {
        public String sessionId;
        public LocalDateTime sessionStartTime;
        public LocalDateTime lastInteractionTime;
        public int totalInteractions = 0;
        public List<EmotionalState> emotionalHistory = new ArrayList<>();
        public boolean recentFrustrationTrend = false;
    }
} 