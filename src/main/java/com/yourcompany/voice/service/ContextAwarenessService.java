package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context Awareness Service for deep UI understanding
 * Analyzes screen content, user focus, and UI state for intelligent responses
 */
@Service
public class ContextAwarenessService {
    
    // Context analysis cache
    private final Map<String, UIContext> contextCache = new ConcurrentHashMap<>();
    
    /**
     * Analyze enhanced UI context for intelligent responses
     */
    public UIContext analyzeContext(String sessionId, Object contextData) {
        try {
            JSONObject context = new JSONObject(contextData.toString());
            
            UIContext uiContext = new UIContext();
            uiContext.sessionId = sessionId;
            uiContext.timestamp = System.currentTimeMillis();
            
            // Basic page information
            uiContext.currentPage = context.optString("page", "/");
            uiContext.pageTitle = context.optString("title", "");
            uiContext.url = context.optString("url", "");
            
            // UI element analysis
            analyzeUIElements(context, uiContext);
            
            // User focus and interaction state
            analyzeUserFocus(context, uiContext);
            
            // Data and loading states
            analyzeDataState(context, uiContext);
            
            // Form analysis
            analyzeFormState(context, uiContext);
            
            // Generate contextual insights
            generateInsights(uiContext);
            
            // Cache for future reference
            contextCache.put(sessionId, uiContext);
            
            return uiContext;
            
        } catch (Exception e) {
            System.err.println("❌ Error analyzing context: " + e.getMessage());
            return createFallbackContext(sessionId);
        }
    }
    
    /**
     * Analyze visible UI elements and interactive components
     */
    private void analyzeUIElements(JSONObject context, UIContext uiContext) {
        // Analyze buttons
        JSONArray buttons = context.optJSONArray("buttons");
        if (buttons != null) {
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject button = buttons.optJSONObject(i);
                if (button != null) {
                    String buttonText = button.optString("text", "").toLowerCase();
                    uiContext.availableActions.add(buttonText);
                    
                    // Categorize button types
                    if (buttonText.contains("add") || buttonText.contains("create") || buttonText.contains("new")) {
                        uiContext.actionTypes.add("creation");
                    } else if (buttonText.contains("delete") || buttonText.contains("remove")) {
                        uiContext.actionTypes.add("deletion");
                    } else if (buttonText.contains("edit") || buttonText.contains("update")) {
                        uiContext.actionTypes.add("modification");
                    } else if (buttonText.contains("search") || buttonText.contains("filter")) {
                        uiContext.actionTypes.add("search");
                    }
                }
            }
        }
        
        // Analyze links and navigation
        JSONArray links = context.optJSONArray("links");
        if (links != null) {
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.optJSONObject(i);
                if (link != null) {
                    String linkText = link.optString("text", "").toLowerCase();
                    String href = link.optString("href", "");
                    uiContext.navigationOptions.put(linkText, href);
                }
            }
        }
    }
    
    /**
     * Analyze user focus and current interaction state
     */
    private void analyzeUserFocus(JSONObject context, UIContext uiContext) {
        String activeElement = context.optString("activeElement", "");
        uiContext.userFocus = activeElement.toLowerCase();
        
        // Determine user intent based on focus
        if (activeElement.contains("input") || activeElement.contains("textarea")) {
            uiContext.userIntent = "data_entry";
        } else if (activeElement.contains("button")) {
            uiContext.userIntent = "action_execution";
        } else if (activeElement.contains("select") || activeElement.contains("dropdown")) {
            uiContext.userIntent = "option_selection";
        } else {
            uiContext.userIntent = "navigation";
        }
        
        // Check for scroll position and viewport
        JSONObject viewport = context.optJSONObject("viewport");
        if (viewport != null) {
            uiContext.scrollPosition = viewport.optInt("scrollTop", 0);
            uiContext.viewportHeight = viewport.optInt("height", 0);
        }
    }
    
    /**
     * Analyze data state, loading, and content
     */
    private void analyzeDataState(JSONObject context, UIContext uiContext) {
        // Loading states
        boolean isLoading = context.optBoolean("isLoading", false);
        uiContext.dataState = isLoading ? "loading" : "loaded";
        
        // Data analysis
        JSONObject dataInfo = context.optJSONObject("dataInfo");
        if (dataInfo != null) {
            uiContext.dataCount = dataInfo.optInt("itemCount", 0);
            uiContext.hasData = uiContext.dataCount > 0;
            uiContext.dataType = dataInfo.optString("type", "");
            
            // Check for empty states
            if (uiContext.dataCount == 0) {
                uiContext.dataState = "empty";
            }
        }
        
        // Error states
        JSONObject errorInfo = context.optJSONObject("errors");
        if (errorInfo != null && errorInfo.length() > 0) {
            uiContext.hasErrors = true;
            uiContext.errorCount = errorInfo.length();
        }
    }
    
    /**
     * Analyze form state and validation
     */
    private void analyzeFormState(JSONObject context, UIContext uiContext) {
        JSONArray forms = context.optJSONArray("forms");
        if (forms != null && forms.length() > 0) {
            uiContext.hasActiveForms = true;
            
            for (int i = 0; i < forms.length(); i++) {
                JSONObject form = forms.optJSONObject(i);
                if (form != null) {
                    FormAnalysis formAnalysis = new FormAnalysis();
                    formAnalysis.formId = form.optString("id", "");
                    formAnalysis.action = form.optString("action", "");
                    formAnalysis.method = form.optString("method", "GET");
                    
                    // Analyze form fields
                    JSONArray fields = form.optJSONArray("fields");
                    if (fields != null) {
                        formAnalysis.fieldCount = fields.length();
                        // Could analyze required fields, validation states, etc.
                    }
                    
                    uiContext.forms.add(formAnalysis);
                }
            }
        }
    }
    
    /**
     * Generate contextual insights and suggestions
     */
    private void generateInsights(UIContext uiContext) {
        List<String> insights = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        // Data state insights
        if ("loading".equals(uiContext.dataState)) {
            insights.add("Content is currently loading");
            suggestions.add("Wait for data to load before proceeding");
        } else if ("empty".equals(uiContext.dataState)) {
            insights.add("No data available on this page");
            if (uiContext.actionTypes.contains("creation")) {
                suggestions.add("Consider creating new content");
            }
            suggestions.add("Check filters or search criteria");
        } else if (uiContext.hasData && uiContext.dataCount > 0) {
            insights.add("Page contains " + uiContext.dataCount + " items");
            if (uiContext.actionTypes.contains("search")) {
                suggestions.add("Use search to find specific items");
            }
        }
        
        // Form insights
        if (uiContext.hasActiveForms) {
            insights.add("Forms available for data entry");
            if ("data_entry".equals(uiContext.userIntent)) {
                suggestions.add("I can help you fill out this form");
            }
        }
        
        // Error insights
        if (uiContext.hasErrors) {
            insights.add("Page has " + uiContext.errorCount + " errors");
            suggestions.add("Address errors before proceeding");
        }
        
        // Focus-based insights
        if ("data_entry".equals(uiContext.userIntent)) {
            insights.add("User is entering data");
            suggestions.add("I can help with form completion");
        }
        
        uiContext.insights = insights;
        uiContext.suggestions = suggestions;
    }
    
    /**
     * Get contextual response based on UI state
     */
    public String getContextualResponse(String sessionId, String baseResponse) {
        UIContext context = contextCache.get(sessionId);
        if (context == null) {
            return baseResponse;
        }
        
        StringBuilder enhancedResponse = new StringBuilder(baseResponse);
        
        // Add context-specific information
        if ("loading".equals(context.dataState)) {
            enhancedResponse.append(" I notice the page is still loading.");
        } else if ("empty".equals(context.dataState)) {
            enhancedResponse.append(" This page appears to be empty.");
        } else if (context.hasErrors) {
            enhancedResponse.append(" I see there are some errors on this page.");
        }
        
        return enhancedResponse.toString();
    }
    
    /**
     * Get proactive suggestions based on context
     */
    public List<String> getProactiveSuggestions(String sessionId) {
        UIContext context = contextCache.get(sessionId);
        if (context == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(context.suggestions);
    }
    
    /**
     * Create fallback context when analysis fails
     */
    private UIContext createFallbackContext(String sessionId) {
        UIContext fallback = new UIContext();
        fallback.sessionId = sessionId;
        fallback.timestamp = System.currentTimeMillis();
        fallback.currentPage = "/";
        fallback.dataState = "unknown";
        fallback.userIntent = "navigation";
        return fallback;
    }
    
    // Inner classes for data structures
    
    public static class UIContext {
        public String sessionId;
        public long timestamp;
        
        // Page information
        public String currentPage;
        public String pageTitle;
        public String url;
        
        // UI elements
        public List<String> availableActions = new ArrayList<>();
        public Set<String> actionTypes = new HashSet<>();
        public Map<String, String> navigationOptions = new HashMap<>();
        
        // User state
        public String userFocus;
        public String userIntent;
        public int scrollPosition;
        public int viewportHeight;
        
        // Data state
        public String dataState;
        public int dataCount;
        public boolean hasData;
        public String dataType;
        public boolean hasErrors;
        public int errorCount;
        
        // Forms
        public boolean hasActiveForms;
        public List<FormAnalysis> forms = new ArrayList<>();
        
        // Generated insights
        public List<String> insights = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
    }
    
    public static class FormAnalysis {
        public String formId;
        public String action;
        public String method;
        public int fieldCount;
        public boolean hasRequiredFields;
        public boolean hasValidationErrors;
    }
} 