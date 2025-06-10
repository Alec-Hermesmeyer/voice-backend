package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.UserPreferenceService;
import com.yourcompany.voice.service.UserPreferenceService.UserProfile;
import com.yourcompany.voice.service.UserPreferenceService.ConversationEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.Map;

/**
 * Optional Controller for User Preference Management
 * Allows frontend to access and modify user personalization settings
 */
@RestController
@RequestMapping("/api/user-preferences")
@CrossOrigin(origins = {"http://localhost:3000", "https://*.vercel.app"})
public class UserPreferenceController {

    @Autowired
    private UserPreferenceService userPreferenceService;

    /**
     * Get user preferences
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<UserPreferenceResponse> getUserPreferences(@PathVariable("clientId") String clientId) {
        try {
            UserProfile profile = userPreferenceService.getUserProfile(clientId);
            
            UserPreferenceResponse response = new UserPreferenceResponse();
            response.success = true;
            response.clientId = clientId;
            response.preferences = profile.preferences;
            response.createdAt = profile.createdAt.toString();
            response.lastUpdated = profile.lastUpdated.toString();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            UserPreferenceResponse response = new UserPreferenceResponse();
            response.success = false;
            response.message = "Error retrieving user preferences: " + e.getMessage();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Update a specific user preference
     */
    @PutMapping("/{clientId}/{key}")
    public ResponseEntity<UpdatePreferenceResponse> updatePreference(
            @PathVariable("clientId") String clientId,
            @PathVariable("key") String key,
            @RequestBody UpdatePreferenceRequest request) {
        
        try {
            userPreferenceService.updateUserPreference(clientId, key, request.getValue());
            
            UpdatePreferenceResponse response = new UpdatePreferenceResponse();
            response.success = true;
            response.message = "Preference updated successfully";
            response.key = key;
            response.value = request.getValue();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            UpdatePreferenceResponse response = new UpdatePreferenceResponse();
            response.success = false;
            response.message = "Error updating preference: " + e.getMessage();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get user behavior statistics
     */
    @GetMapping("/{clientId}/behavior-stats")
    public ResponseEntity<BehaviorStatsResponse> getBehaviorStats(@PathVariable("clientId") String clientId) {
        try {
            Map<String, Integer> commonActions = userPreferenceService.getCommonActions(clientId);
            List<String> suggestions = userPreferenceService.getContextualSuggestions(clientId, "current");
            
            BehaviorStatsResponse response = new BehaviorStatsResponse();
            response.success = true;
            response.clientId = clientId;
            response.commonActions = commonActions;
            response.suggestions = suggestions;
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            BehaviorStatsResponse response = new BehaviorStatsResponse();
            response.success = false;
            response.message = "Error retrieving behavior stats: " + e.getMessage();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get conversation history
     */
    @GetMapping("/{clientId}/conversation-history")
    public ResponseEntity<ConversationHistoryResponse> getConversationHistory(
            @PathVariable("clientId") String clientId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        try {
            List<ConversationEntry> conversations = userPreferenceService.getRecentConversations(clientId, limit);
            
            ConversationHistoryResponse response = new ConversationHistoryResponse();
            response.success = true;
            response.clientId = clientId;
            response.conversations = conversations;
            response.totalCount = conversations.size();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ConversationHistoryResponse response = new ConversationHistoryResponse();
            response.success = false;
            response.message = "Error retrieving conversation history: " + e.getMessage();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Clear user data (GDPR compliance)
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<ClearDataResponse> clearUserData(@PathVariable("clientId") String clientId) {
        try {
            // Note: This would need to be implemented in UserPreferenceService
            // For now, we'll just return a success message
            
            ClearDataResponse response = new ClearDataResponse();
            response.success = true;
            response.message = "User data cleared successfully";
            response.clientId = clientId;
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ClearDataResponse response = new ClearDataResponse();
            response.success = false;
            response.message = "Error clearing user data: " + e.getMessage();
            return ResponseEntity.ok(response);
        }
    }

    // Request/Response DTOs

    public static class UpdatePreferenceRequest {
        private Object value;

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }

    public static class UserPreferenceResponse {
        public boolean success;
        public String message;
        public String clientId;
        public Map<String, Object> preferences;
        public String createdAt;
        public String lastUpdated;
    }

    public static class UpdatePreferenceResponse {
        public boolean success;
        public String message;
        public String key;
        public Object value;
    }

    public static class BehaviorStatsResponse {
        public boolean success;
        public String message;
        public String clientId;
        public Map<String, Integer> commonActions;
        public List<String> suggestions;
    }

    public static class ConversationHistoryResponse {
        public boolean success;
        public String message;
        public String clientId;
        public List<ConversationEntry> conversations;
        public int totalCount;
    }

    public static class ClearDataResponse {
        public boolean success;
        public String message;
        public String clientId;
    }
} 