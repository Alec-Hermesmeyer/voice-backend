package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Predictive Assistance Service for proactive help based on user patterns
 * Analyzes user behavior to provide predictive suggestions and assistance
 */
@Service
public class PredictiveAssistanceService {
    
    @Autowired
    private UserPreferenceService userPreferenceService;
    
    // Pattern tracking and predictive models
    private final Map<String, UserBehaviorPattern> behaviorPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<PredictiveInsight>> predictiveInsights = new ConcurrentHashMap<>();
    
    // Common patterns for proactive assistance
    private final Map<String, List<String>> commonWorkflows = Map.of(
        "data_entry", List.of("navigate", "create", "validate", "submit"),
        "search_filter", List.of("navigate", "search", "filter", "view"),
        "management", List.of("view", "edit", "update", "save"),
        "analysis", List.of("navigate", "filter", "export", "download")
    );
    
    /**
     * Analyze user behavior and generate predictive insights
     */
    public PredictiveInsight analyzeBehaviorAndPredict(String sessionId, String action, Object contextData) {
        UserBehaviorPattern pattern = getOrCreateBehaviorPattern(sessionId);
        
        // Record current action
        recordUserAction(pattern, action, contextData);
        
        // Analyze patterns
        analyzeUserPatterns(pattern);
        
        // Generate predictive insights
        PredictiveInsight insight = generatePredictiveInsight(pattern, contextData);
        
        // Cache insights for quick access
        cacheInsight(sessionId, insight);
        
        return insight;
    }
    
    /**
     * Record user action for pattern analysis
     */
    private void recordUserAction(UserBehaviorPattern pattern, String action, Object contextData) {
        UserAction userAction = new UserAction();
        userAction.action = action;
        userAction.timestamp = LocalDateTime.now();
        userAction.context = contextData.toString();
        
        pattern.actions.add(userAction);
        
        // Keep only recent actions (last 50)
        if (pattern.actions.size() > 50) {
            pattern.actions.remove(0);
        }
        
        // Update action frequency
        pattern.actionFrequency.merge(action, 1, Integer::sum);
        
        // Update timing patterns
        updateTimingPatterns(pattern, action);
    }
    
    /**
     * Update timing patterns for predictive modeling
     */
    private void updateTimingPatterns(UserBehaviorPattern pattern, String action) {
        if (pattern.actions.size() >= 2) {
            UserAction current = pattern.actions.get(pattern.actions.size() - 1);
            UserAction previous = pattern.actions.get(pattern.actions.size() - 2);
            
            long timeDiff = ChronoUnit.SECONDS.between(previous.timestamp, current.timestamp);
            
            // Track action sequences and timing
            String sequence = previous.action + " -> " + action;
            pattern.sequenceFrequency.merge(sequence, 1, Integer::sum);
            pattern.sequenceTiming.put(sequence, timeDiff);
        }
    }
    
    /**
     * Analyze user patterns for predictive insights
     */
    private void analyzeUserPatterns(UserBehaviorPattern pattern) {
        // Identify workflow patterns
        identifyWorkflowPatterns(pattern);
        
        // Analyze time-based patterns
        analyzeTimePatterns(pattern);
        
        // Detect inefficiencies
        detectInefficiencies(pattern);
        
        // Identify repetitive behaviors
        identifyRepetitiveBehaviors(pattern);
    }
    
    /**
     * Identify workflow patterns from user actions
     */
    private void identifyWorkflowPatterns(UserBehaviorPattern pattern) {
        if (pattern.actions.size() < 3) {
            return;
        }
        
        // Get last 5 actions to analyze current workflow
        List<UserAction> recentActions = pattern.actions.subList(
            Math.max(0, pattern.actions.size() - 5), 
            pattern.actions.size()
        );
        
        List<String> actionSequence = new ArrayList<>();
        for (UserAction action : recentActions) {
            actionSequence.add(action.action);
        }
        
        // Match against common workflows
        for (Map.Entry<String, List<String>> workflow : commonWorkflows.entrySet()) {
            int matchScore = calculateWorkflowMatch(actionSequence, workflow.getValue());
            if (matchScore > 50) {
                pattern.currentWorkflow = workflow.getKey();
                pattern.workflowProgress = calculateWorkflowProgress(actionSequence, workflow.getValue());
                break;
            }
        }
    }
    
    /**
     * Calculate how well current actions match a workflow pattern
     */
    private int calculateWorkflowMatch(List<String> userActions, List<String> workflowPattern) {
        int matches = 0;
        int total = Math.min(userActions.size(), workflowPattern.size());
        
        for (int i = 0; i < total; i++) {
            String userAction = userActions.get(userActions.size() - total + i);
            String workflowAction = workflowPattern.get(i);
            
            if (userAction.contains(workflowAction) || workflowAction.contains(userAction)) {
                matches++;
            }
        }
        
        return total > 0 ? (matches * 100) / total : 0;
    }
    
    /**
     * Calculate progress through identified workflow
     */
    private double calculateWorkflowProgress(List<String> userActions, List<String> workflowPattern) {
        int maxProgress = 0;
        
        for (int i = 0; i < workflowPattern.size(); i++) {
            String workflowStep = workflowPattern.get(i);
            boolean found = false;
            
            for (String userAction : userActions) {
                if (userAction.contains(workflowStep)) {
                    found = true;
                    break;
                }
            }
            
            if (found) {
                maxProgress = i + 1;
            }
        }
        
        return ((double) maxProgress / workflowPattern.size()) * 100;
    }
    
    /**
     * Analyze time-based patterns
     */
    private void analyzeTimePatterns(UserBehaviorPattern pattern) {
        if (pattern.actions.isEmpty()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = pattern.actions.get(0).timestamp;
        
        pattern.sessionDuration = ChronoUnit.MINUTES.between(sessionStart, now);
        pattern.averageActionInterval = calculateAverageActionInterval(pattern);
        
        // Detect if user is rushing or taking their time
        if (pattern.averageActionInterval < 10) {
            pattern.userPace = "fast";
        } else if (pattern.averageActionInterval > 60) {
            pattern.userPace = "slow";
        } else {
            pattern.userPace = "normal";
        }
    }
    
    /**
     * Calculate average interval between actions
     */
    private double calculateAverageActionInterval(UserBehaviorPattern pattern) {
        if (pattern.actions.size() < 2) {
            return 0;
        }
        
        long totalInterval = 0;
        for (int i = 1; i < pattern.actions.size(); i++) {
            long interval = ChronoUnit.SECONDS.between(
                pattern.actions.get(i-1).timestamp,
                pattern.actions.get(i).timestamp
            );
            totalInterval += interval;
        }
        
        return (double) totalInterval / (pattern.actions.size() - 1);
    }
    
    /**
     * Detect inefficiencies in user behavior
     */
    private void detectInefficiencies(UserBehaviorPattern pattern) {
        List<String> inefficiencies = new ArrayList<>();
        
        // Check for excessive back-and-forth navigation
        long backNavigationCount = 0;
        for (UserAction action : pattern.actions) {
            if (action.action.contains("navigate") || action.action.contains("back")) {
                backNavigationCount++;
            }
        }
        
        if (backNavigationCount > pattern.actions.size() * 0.3) {
            inefficiencies.add("excessive_navigation");
        }
        
        // Check for repeated failed attempts
        long errorCount = 0;
        for (UserAction action : pattern.actions) {
            if (action.action.contains("error") || action.action.contains("retry")) {
                errorCount++;
            }
        }
        
        if (errorCount > 3) {
            inefficiencies.add("repeated_errors");
        }
        
        // Check for long pauses (possible confusion)
        boolean hasLongPauses = false;
        for (Long timing : pattern.sequenceTiming.values()) {
            if (timing > 120) { // 2 minutes
                hasLongPauses = true;
                break;
            }
        }
        
        if (hasLongPauses) {
            inefficiencies.add("long_pauses");
        }
        
        pattern.detectedInefficiencies = inefficiencies;
    }
    
    /**
     * Identify repetitive behaviors
     */
    private void identifyRepetitiveBehaviors(UserBehaviorPattern pattern) {
        List<String> repetitiveBehaviors = new ArrayList<>();
        
        // Check for repeated actions
        for (Map.Entry<String, Integer> entry : pattern.actionFrequency.entrySet()) {
            if (entry.getValue() > 5) {
                repetitiveBehaviors.add("repeated_" + entry.getKey());
            }
        }
        
        // Check for circular navigation patterns
        if (detectCircularNavigation(pattern)) {
            repetitiveBehaviors.add("circular_navigation");
        }
        
        pattern.repetitiveBehaviors = repetitiveBehaviors;
    }
    
    /**
     * Detect circular navigation patterns
     */
    private boolean detectCircularNavigation(UserBehaviorPattern pattern) {
        if (pattern.actions.size() < 6) {
            return false;
        }
        
        // Look for A->B->C->A type patterns in recent actions
        List<UserAction> recent = pattern.actions.subList(
            Math.max(0, pattern.actions.size() - 6), 
            pattern.actions.size()
        );
        
        Set<String> visitedPages = new HashSet<>();
        for (UserAction action : recent) {
            if (action.action.contains("navigate")) {
                if (visitedPages.contains(action.action)) {
                    return true;
                }
                visitedPages.add(action.action);
            }
        }
        
        return false;
    }
    
    /**
     * Generate predictive insight based on patterns
     */
    private PredictiveInsight generatePredictiveInsight(UserBehaviorPattern pattern, Object contextData) {
        PredictiveInsight insight = new PredictiveInsight();
        insight.timestamp = LocalDateTime.now();
        insight.sessionId = pattern.sessionId;
        insight.confidence = 50; // Base confidence
        
        // Generate predictions based on workflow
        generateWorkflowPredictions(pattern, insight);
        
        // Generate efficiency suggestions
        generateEfficiencySuggestions(pattern, insight);
        
        // Generate proactive help
        generateProactiveHelp(pattern, insight, contextData);
        
        // Generate next action predictions
        generateNextActionPredictions(pattern, insight);
        
        // Adjust confidence based on pattern strength
        adjustConfidence(pattern, insight);
        
        return insight;
    }
    
    /**
     * Generate workflow-based predictions
     */
    private void generateWorkflowPredictions(UserBehaviorPattern pattern, PredictiveInsight insight) {
        if (pattern.currentWorkflow != null) {
            List<String> workflowSteps = commonWorkflows.get(pattern.currentWorkflow);
            
            if (workflowSteps != null && pattern.workflowProgress < 100) {
                int nextStepIndex = (int) Math.ceil(pattern.workflowProgress / 100.0 * workflowSteps.size());
                
                if (nextStepIndex < workflowSteps.size()) {
                    String nextStep = workflowSteps.get(nextStepIndex);
                    insight.predictedNextAction = nextStep;
                    insight.suggestions.add("Next step in " + pattern.currentWorkflow + " workflow: " + nextStep);
                    insight.confidence += 20;
                }
            }
        }
    }
    
    /**
     * Generate efficiency improvement suggestions
     */
    private void generateEfficiencySuggestions(UserBehaviorPattern pattern, PredictiveInsight insight) {
        for (String inefficiency : pattern.detectedInefficiencies) {
            switch (inefficiency) {
                case "excessive_navigation":
                    insight.suggestions.add("I notice you're navigating back and forth frequently. Would you like me to help you find a more direct path?");
                    break;
                case "repeated_errors":
                    insight.suggestions.add("You seem to be encountering repeated issues. Let me help you troubleshoot this.");
                    break;
                case "long_pauses":
                    insight.suggestions.add("I noticed some long pauses. Would you like me to explain what's available on this page?");
                    break;
            }
        }
    }
    
    /**
     * Generate proactive help based on context
     */
    private void generateProactiveHelp(UserBehaviorPattern pattern, PredictiveInsight insight, Object contextData) {
        String context = contextData.toString().toLowerCase();
        
        // Form assistance
        if (context.contains("form") && !pattern.actionFrequency.containsKey("form_submit")) {
            insight.proactiveHelp.add("I can help you fill out this form step by step");
            insight.confidence += 15;
        }
        
        // Data assistance
        if (context.contains("empty") || context.contains("no data")) {
            insight.proactiveHelp.add("This page appears empty. Would you like me to help you add some data?");
            insight.confidence += 10;
        }
        
        // Error assistance
        if (context.contains("error")) {
            insight.proactiveHelp.add("I notice there's an error. Let me help you resolve it");
            insight.confidence += 25;
        }
        
        // Loading assistance
        if (context.contains("loading")) {
            insight.proactiveHelp.add("While this loads, I can explain what you'll see next");
            insight.confidence += 5;
        }
    }
    
    /**
     * Generate next action predictions based on sequence patterns
     */
    private void generateNextActionPredictions(UserBehaviorPattern pattern, PredictiveInsight insight) {
        if (pattern.actions.isEmpty()) {
            return;
        }
        
        String lastAction = pattern.actions.get(pattern.actions.size() - 1).action;
        
        // Find most common next actions
        String mostLikelyNext = null;
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : pattern.sequenceFrequency.entrySet()) {
            if (entry.getKey().startsWith(lastAction + " ->") && entry.getValue() > maxCount) {
                mostLikelyNext = entry.getKey().split(" -> ")[1];
                maxCount = entry.getValue();
            }
        }
        
        if (mostLikelyNext != null) {
            insight.predictedNextAction = mostLikelyNext;
            insight.nextActionProbability = calculateActionProbability(pattern, lastAction, mostLikelyNext);
            insight.confidence += 10;
        }
    }
    
    /**
     * Calculate probability of next action based on historical patterns
     */
    private double calculateActionProbability(UserBehaviorPattern pattern, String currentAction, String nextAction) {
        String sequence = currentAction + " -> " + nextAction;
        int sequenceCount = pattern.sequenceFrequency.getOrDefault(sequence, 0);
        
        int totalFromCurrent = 0;
        for (Map.Entry<String, Integer> entry : pattern.sequenceFrequency.entrySet()) {
            if (entry.getKey().startsWith(currentAction + " ->")) {
                totalFromCurrent += entry.getValue();
            }
        }
        
        return totalFromCurrent > 0 ? ((double) sequenceCount / totalFromCurrent) * 100 : 0;
    }
    
    /**
     * Adjust confidence based on pattern strength
     */
    private void adjustConfidence(UserBehaviorPattern pattern, PredictiveInsight insight) {
        // More data = higher confidence
        if (pattern.actions.size() > 20) {
            insight.confidence += 15;
        } else if (pattern.actions.size() > 10) {
            insight.confidence += 10;
        }
        
        // Clear workflow patterns = higher confidence
        if (pattern.currentWorkflow != null && pattern.workflowProgress > 50) {
            insight.confidence += 20;
        }
        
        // Consistent timing = higher confidence
        if ("normal".equals(pattern.userPace)) {
            insight.confidence += 10;
        }
        
        // Cap confidence at 95%
        insight.confidence = Math.min(95, insight.confidence);
    }
    
    /**
     * Get proactive suggestions for current context
     */
    public List<String> getProactiveSuggestions(String sessionId, Object contextData) {
        List<PredictiveInsight> insights = predictiveInsights.get(sessionId);
        if (insights == null || insights.isEmpty()) {
            return new ArrayList<>();
        }
        
        PredictiveInsight latestInsight = insights.get(insights.size() - 1);
        
        List<String> allSuggestions = new ArrayList<>();
        allSuggestions.addAll(latestInsight.suggestions);
        allSuggestions.addAll(latestInsight.proactiveHelp);
        
        // Filter suggestions based on confidence
        List<String> filteredSuggestions = new ArrayList<>();
        if (latestInsight.confidence > 60) {
            filteredSuggestions.addAll(allSuggestions);
        }
        
        return filteredSuggestions;
    }
    
    /**
     * Get predicted next action
     */
    public String getPredictedNextAction(String sessionId) {
        List<PredictiveInsight> insights = predictiveInsights.get(sessionId);
        if (insights == null || insights.isEmpty()) {
            return null;
        }
        
        PredictiveInsight latestInsight = insights.get(insights.size() - 1);
        return latestInsight.confidence > 70 ? latestInsight.predictedNextAction : null;
    }
    
    /**
     * Cache insight for quick access
     */
    private void cacheInsight(String sessionId, PredictiveInsight insight) {
        List<PredictiveInsight> insights = predictiveInsights.computeIfAbsent(sessionId, k -> new ArrayList<>());
        insights.add(insight);
        
        // Keep only recent insights (last 10)
        if (insights.size() > 10) {
            insights.remove(0);
        }
    }
    
    /**
     * Get or create behavior pattern for user
     */
    private UserBehaviorPattern getOrCreateBehaviorPattern(String sessionId) {
        return behaviorPatterns.computeIfAbsent(sessionId, k -> {
            UserBehaviorPattern pattern = new UserBehaviorPattern();
            pattern.sessionId = sessionId;
            pattern.startTime = LocalDateTime.now();
            return pattern;
        });
    }
    
    /**
     * Clear patterns (end of session)
     */
    public void clearBehaviorPatterns(String sessionId) {
        behaviorPatterns.remove(sessionId);
        predictiveInsights.remove(sessionId);
    }
    
    // Inner classes for data structures
    
    public static class UserBehaviorPattern {
        public String sessionId;
        public LocalDateTime startTime;
        public List<UserAction> actions = new ArrayList<>();
        public Map<String, Integer> actionFrequency = new HashMap<>();
        public Map<String, Integer> sequenceFrequency = new HashMap<>();
        public Map<String, Long> sequenceTiming = new HashMap<>();
        
        // Workflow analysis
        public String currentWorkflow;
        public double workflowProgress = 0;
        
        // Timing analysis
        public long sessionDuration = 0;
        public double averageActionInterval = 0;
        public String userPace = "normal"; // fast, normal, slow
        
        // Pattern detection
        public List<String> detectedInefficiencies = new ArrayList<>();
        public List<String> repetitiveBehaviors = new ArrayList<>();
    }
    
    public static class UserAction {
        public String action;
        public LocalDateTime timestamp;
        public String context;
    }
    
    public static class PredictiveInsight {
        public LocalDateTime timestamp;
        public String sessionId;
        public int confidence = 50; // 0-100
        
        // Predictions
        public String predictedNextAction;
        public double nextActionProbability = 0;
        
        // Suggestions and help
        public List<String> suggestions = new ArrayList<>();
        public List<String> proactiveHelp = new ArrayList<>();
        
        // Workflow insights
        public String currentWorkflowStep;
        public String suggestedNextStep;
    }
} 