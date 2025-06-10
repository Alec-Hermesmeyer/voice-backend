package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User Preference Service for cross-session memory and personalization
 * Stores user preferences, behavior patterns, and conversation context across sessions
 */
@Service
public class UserPreferenceService {
    
    private static final String USER_DATA_DIR = "./user-data";
    private static final String PREFERENCES_SUFFIX = "_preferences.json";
    private static final String CONVERSATION_HISTORY_SUFFIX = "_conversations.json";
    private static final String BEHAVIOR_PATTERNS_SUFFIX = "_patterns.json";
    
    // In-memory cache for fast access
    private final Map<String, UserProfile> userProfileCache = new ConcurrentHashMap<>();
    
    public UserPreferenceService() {
        initializeUserDataStore();
    }
    
    /**
     * Initialize user data directory structure
     */
    private void initializeUserDataStore() {
        try {
            Path userDataPath = Paths.get(USER_DATA_DIR);
            if (!Files.exists(userDataPath)) {
                Files.createDirectories(userDataPath);
                System.out.println("✅ Created user data directory: " + USER_DATA_DIR);
            }
        } catch (IOException e) {
            System.err.println("❌ Error creating user data directory: " + e.getMessage());
        }
    }
    
    /**
     * Get or create user profile
     */
    public UserProfile getUserProfile(String clientId) {
        UserProfile profile = userProfileCache.get(clientId);
        if (profile == null) {
            profile = loadUserProfile(clientId);
            if (profile == null) {
                profile = createDefaultProfile(clientId);
            }
            userProfileCache.put(clientId, profile);
        }
        return profile;
    }
    
    /**
     * Update user preferences
     */
    public void updateUserPreference(String clientId, String key, Object value) {
        UserProfile profile = getUserProfile(clientId);
        profile.preferences.put(key, value);
        profile.lastUpdated = LocalDateTime.now();
        saveUserProfile(profile);
    }
    
    /**
     * Track user behavior patterns
     */
    public void trackUserBehavior(String clientId, String action, Map<String, Object> context) {
        UserProfile profile = getUserProfile(clientId);
        
        // Update behavior patterns
        BehaviorPattern pattern = new BehaviorPattern(action, context, LocalDateTime.now());
        profile.behaviorPatterns.add(pattern);
        
        // Keep only recent patterns (last 100)
        if (profile.behaviorPatterns.size() > 100) {
            profile.behaviorPatterns.remove(0);
        }
        
        // Update common patterns
        profile.commonActions.merge(action, 1, Integer::sum);
        
        profile.lastUpdated = LocalDateTime.now();
        saveUserProfile(profile);
    }
    
    /**
     * Add conversation to cross-session history
     */
    public void addConversationHistory(String clientId, String input, String response, String context) {
        UserProfile profile = getUserProfile(clientId);
        
        ConversationEntry entry = new ConversationEntry(input, response, context, LocalDateTime.now());
        profile.conversationHistory.add(entry);
        
        // Keep only recent conversations (last 50 across all sessions)
        if (profile.conversationHistory.size() > 50) {
            profile.conversationHistory.remove(0);
        }
        
        profile.lastUpdated = LocalDateTime.now();
        saveUserProfile(profile);
    }
    
    /**
     * Get user's recent conversation context for better understanding
     */
    public List<ConversationEntry> getRecentConversations(String clientId, int limit) {
        UserProfile profile = getUserProfile(clientId);
        List<ConversationEntry> conversations = profile.conversationHistory;
        
        if (conversations.size() <= limit) {
            return new ArrayList<>(conversations);
        }
        
        return new ArrayList<>(conversations.subList(conversations.size() - limit, conversations.size()));
    }
    
    /**
     * Get user's most common actions for proactive suggestions
     */
    public Map<String, Integer> getCommonActions(String clientId) {
        UserProfile profile = getUserProfile(clientId);
        return new HashMap<>(profile.commonActions);
    }
    
    /**
     * Get user preference value
     */
    public Object getUserPreference(String clientId, String key, Object defaultValue) {
        UserProfile profile = getUserProfile(clientId);
        return profile.preferences.getOrDefault(key, defaultValue);
    }
    
    /**
     * Check if user has shown preference for certain UI patterns
     */
    public boolean hasPreferencePattern(String clientId, String pattern) {
        UserProfile profile = getUserProfile(clientId);
        return profile.behaviorPatterns.stream()
                .anyMatch(p -> p.action.toLowerCase().contains(pattern.toLowerCase()));
    }
    
    /**
     * Get contextual suggestions based on user history
     */
    public List<String> getContextualSuggestions(String clientId, String currentContext) {
        UserProfile profile = getUserProfile(clientId);
        List<String> suggestions = new ArrayList<>();
        
        // Find common actions in similar contexts
        profile.behaviorPatterns.stream()
                .filter(p -> p.context.getOrDefault("page", "").toString().equals(currentContext))
                .map(p -> p.action)
                .distinct()
                .limit(3)
                .forEach(suggestions::add);
        
        // Add global common actions if we need more suggestions
        if (suggestions.size() < 3) {
            profile.commonActions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .filter(action -> !suggestions.contains(action))
                    .limit(3 - suggestions.size())
                    .forEach(suggestions::add);
        }
        
        return suggestions;
    }
    
    /**
     * Create default user profile
     */
    private UserProfile createDefaultProfile(String clientId) {
        UserProfile profile = new UserProfile();
        profile.clientId = clientId;
        profile.createdAt = LocalDateTime.now();
        profile.lastUpdated = LocalDateTime.now();
        
        // Set default preferences
        profile.preferences.put("voice_speed", "normal");
        profile.preferences.put("response_style", "helpful");
        profile.preferences.put("confirmation_level", "medium");
        profile.preferences.put("preferred_voice", "alloy");
        
        return profile;
    }
    
    /**
     * Load user profile from file
     */
    private UserProfile loadUserProfile(String clientId) {
        try {
            Path prefsPath = Paths.get(USER_DATA_DIR, clientId + PREFERENCES_SUFFIX);
            if (!Files.exists(prefsPath)) {
                return null;
            }
            
            String content = Files.readString(prefsPath);
            JSONObject json = new JSONObject(content);
            
            UserProfile profile = new UserProfile();
            profile.clientId = clientId;
            profile.createdAt = LocalDateTime.parse(json.getString("createdAt"));
            profile.lastUpdated = LocalDateTime.parse(json.getString("lastUpdated"));
            
            // Load preferences
            JSONObject prefs = json.getJSONObject("preferences");
            Iterator<String> keys = prefs.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                profile.preferences.put(key, prefs.get(key));
            }
            
            // Load behavior patterns
            JSONArray patterns = json.optJSONArray("behaviorPatterns");
            if (patterns != null) {
                for (int i = 0; i < patterns.length(); i++) {
                    JSONObject patternJson = patterns.getJSONObject(i);
                    BehaviorPattern pattern = new BehaviorPattern(
                        patternJson.getString("action"),
                        jsonToMap(patternJson.getJSONObject("context")),
                        LocalDateTime.parse(patternJson.getString("timestamp"))
                    );
                    profile.behaviorPatterns.add(pattern);
                }
            }
            
            // Load conversation history
            JSONArray conversations = json.optJSONArray("conversationHistory");
            if (conversations != null) {
                for (int i = 0; i < conversations.length(); i++) {
                    JSONObject convJson = conversations.getJSONObject(i);
                    ConversationEntry entry = new ConversationEntry(
                        convJson.getString("input"),
                        convJson.getString("response"),
                        convJson.getString("context"),
                        LocalDateTime.parse(convJson.getString("timestamp"))
                    );
                    profile.conversationHistory.add(entry);
                }
            }
            
            // Load common actions
            JSONObject commonActions = json.optJSONObject("commonActions");
            if (commonActions != null) {
                Iterator<String> actionKeys = commonActions.keys();
                while (actionKeys.hasNext()) {
                    String key = actionKeys.next();
                    profile.commonActions.put(key, commonActions.getInt(key));
                }
            }
            
            return profile;
            
        } catch (Exception e) {
            System.err.println("❌ Error loading user profile for " + clientId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Save user profile to file
     */
    private void saveUserProfile(UserProfile profile) {
        try {
            JSONObject json = new JSONObject();
            json.put("clientId", profile.clientId);
            json.put("createdAt", profile.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            json.put("lastUpdated", profile.lastUpdated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Save preferences
            JSONObject prefs = new JSONObject();
            for (Map.Entry<String, Object> entry : profile.preferences.entrySet()) {
                prefs.put(entry.getKey(), entry.getValue());
            }
            json.put("preferences", prefs);
            
            // Save behavior patterns
            JSONArray patterns = new JSONArray();
            for (BehaviorPattern pattern : profile.behaviorPatterns) {
                JSONObject patternJson = new JSONObject();
                patternJson.put("action", pattern.action);
                patternJson.put("context", new JSONObject(pattern.context));
                patternJson.put("timestamp", pattern.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                patterns.put(patternJson);
            }
            json.put("behaviorPatterns", patterns);
            
            // Save conversation history
            JSONArray conversations = new JSONArray();
            for (ConversationEntry entry : profile.conversationHistory) {
                JSONObject convJson = new JSONObject();
                convJson.put("input", entry.input);
                convJson.put("response", entry.response);
                convJson.put("context", entry.context);
                convJson.put("timestamp", entry.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                conversations.put(convJson);
            }
            json.put("conversationHistory", conversations);
            
            // Save common actions
            JSONObject commonActions = new JSONObject();
            for (Map.Entry<String, Integer> entry : profile.commonActions.entrySet()) {
                commonActions.put(entry.getKey(), entry.getValue());
            }
            json.put("commonActions", commonActions);
            
            Path filePath = Paths.get(USER_DATA_DIR, profile.clientId + PREFERENCES_SUFFIX);
            Files.writeString(filePath, json.toString(2));
            
        } catch (Exception e) {
            System.err.println("❌ Error saving user profile for " + profile.clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Convert JSONObject to Map
     */
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                map.put(key, json.get(key));
            } catch (JSONException e) {
                System.err.println("❌ Error reading JSON key " + key + ": " + e.getMessage());
            }
        }
        return map;
    }
    
    // Inner classes for data structures
    
    public static class UserProfile {
        public String clientId;
        public LocalDateTime createdAt;
        public LocalDateTime lastUpdated;
        public Map<String, Object> preferences = new HashMap<>();
        public List<BehaviorPattern> behaviorPatterns = new ArrayList<>();
        public List<ConversationEntry> conversationHistory = new ArrayList<>();
        public Map<String, Integer> commonActions = new HashMap<>();
    }
    
    public static class BehaviorPattern {
        public final String action;
        public final Map<String, Object> context;
        public final LocalDateTime timestamp;
        
        public BehaviorPattern(String action, Map<String, Object> context, LocalDateTime timestamp) {
            this.action = action;
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
            this.timestamp = timestamp;
        }
    }
    
    public static class ConversationEntry {
        public final String input;
        public final String response;
        public final String context;
        public final LocalDateTime timestamp;
        
        public ConversationEntry(String input, String response, String context, LocalDateTime timestamp) {
            this.input = input;
            this.response = response;
            this.context = context;
            this.timestamp = timestamp;
        }
    }
} 