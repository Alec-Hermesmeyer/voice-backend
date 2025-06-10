package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UIControlService {
    
    // Store UI context mappings for different pages/contexts
    private final Map<String, UIContext> contextMappings = new ConcurrentHashMap<>();
    
    // Store command-to-action mappings
    private final Map<String, ActionMapping> commandMappings = new ConcurrentHashMap<>();
    
    public UIControlService() {
        initializeDefaultMappings();
    }
    
    /**
     * Initialize default UI context and command mappings
     */
    private void initializeDefaultMappings() {
        // Navigation context (global)
        UIContext navigationContext = new UIContext("navigation", "Navigation");
        navigationContext.addAction("navigate", "/", "Home", "Navigate to home page");
        navigationContext.addAction("navigate", "/dashboard", "Dashboard", "Navigate to dashboard");
        navigationContext.addAction("navigate", "/contracts", "Contracts", "Navigate to contracts page");
        navigationContext.addAction("navigate", "/insurance", "Insurance", "Navigate to insurance page");
        navigationContext.addAction("navigate", "/records", "Records", "Navigate to records page");
        navigationContext.addAction("navigate", "/settings", "Settings", "Navigate to settings page");
        navigationContext.addAction("navigate", "/profile", "Profile", "Navigate to profile page");
        navigationContext.addAction("navigate", "/fast-rag", "Fast RAG", "Navigate to fast RAG page");
        navigationContext.addAction("navigate", "/deep-rag", "Deep RAG", "Navigate to deep RAG page");
        contextMappings.put("navigation", navigationContext);
        
        // Home page context
        UIContext homeContext = new UIContext("home", "Home Page");
        homeContext.addAction("click", "nav-login", "Login button", "Navigate to login page");
        homeContext.addAction("click", "nav-signup", "Sign up button", "Navigate to sign up page");
        homeContext.addAction("click", "hero-cta", "Get started button", "Main call-to-action");
        homeContext.addAction("scroll", "features", "Features section", "Scroll to features");
        contextMappings.put("home", homeContext);
        
        // Login page context
        UIContext loginContext = new UIContext("login", "Login Page");
        loginContext.addAction("focus", "email-input", "Email field", "Focus on email input");
        loginContext.addAction("focus", "password-input", "Password field", "Focus on password input");
        loginContext.addAction("type", "email-input", "Email field", "Type in email field");
        loginContext.addAction("type", "password-input", "Password field", "Type in password field");
        loginContext.addAction("click", "login-submit", "Login button", "Submit login form");
        loginContext.addAction("click", "forgot-password", "Forgot password link", "Go to password reset");
        contextMappings.put("login", loginContext);
        
        // Dashboard context
        UIContext dashboardContext = new UIContext("dashboard", "Dashboard");
        dashboardContext.addAction("click", "sidebar-nav", "Navigation menu", "Open/close sidebar");
        dashboardContext.addAction("click", "profile-menu", "Profile menu", "Open profile dropdown");
        dashboardContext.addAction("click", "new-project", "New project button", "Create new project");
        dashboardContext.addAction("navigate", "settings", "Settings page", "Navigate to settings");
        contextMappings.put("dashboard", dashboardContext);
        
        // Form context (generic)
        UIContext formContext = new UIContext("form", "Form Page");
        formContext.addAction("focus", "*-input", "Input fields", "Focus on input field");
        formContext.addAction("type", "*-input", "Input fields", "Type in input field");
        formContext.addAction("click", "submit-button", "Submit button", "Submit form");
        formContext.addAction("click", "cancel-button", "Cancel button", "Cancel form");
        formContext.addAction("clear", "*-input", "Input fields", "Clear input field");
        contextMappings.put("form", formContext);
        
        // Initialize command mappings
        initializeCommandMappings();
    }
    
    /**
     * Initialize voice command to action mappings
     */
    private void initializeCommandMappings() {
        // Click commands
        addCommandMapping("click", "click", Arrays.asList("click", "press", "tap", "hit"));
        addCommandMapping("click_submit", "click", Arrays.asList("submit", "send", "go", "enter"));
        addCommandMapping("click_cancel", "click", Arrays.asList("cancel", "back", "close"));
        addCommandMapping("click_login", "click", Arrays.asList("login", "sign in"));
        addCommandMapping("click_signup", "click", Arrays.asList("sign up", "register", "create account"));
        
        // Navigation commands
        addCommandMapping("navigate_home", "navigate", Arrays.asList("go home", "home page", "main page"));
        addCommandMapping("navigate_dashboard", "navigate", Arrays.asList("dashboard", "main dashboard"));
        addCommandMapping("navigate_settings", "navigate", Arrays.asList("settings", "preferences"));
        addCommandMapping("navigate_profile", "navigate", Arrays.asList("profile", "my profile"));
        
        // Enhanced navigation commands
        addCommandMapping("navigate", "navigate", Arrays.asList(
            "go to", "navigate to", "open", "show me", "take me to", "switch to"
        ));
        
        // Page-specific navigation patterns
        addCommandMapping("nav_home", "navigate", Arrays.asList("home", "main page", "start"));
        addCommandMapping("nav_dashboard", "navigate", Arrays.asList("dashboard", "main dashboard", "overview"));
        addCommandMapping("nav_contracts", "navigate", Arrays.asList("contracts", "contract analyst", "contract page"));
        addCommandMapping("nav_insurance", "navigate", Arrays.asList("insurance", "insurance broker", "policies"));
        addCommandMapping("nav_records", "navigate", Arrays.asList("records", "open records", "record search"));
        addCommandMapping("nav_settings", "navigate", Arrays.asList("settings", "preferences", "configuration"));
        addCommandMapping("nav_profile", "navigate", Arrays.asList("profile", "my account", "account settings"));
        addCommandMapping("nav_fast_rag", "navigate", Arrays.asList("fast rag", "fast RAG", "quick rag", "fast retrieval"));
        addCommandMapping("nav_deep_rag", "navigate", Arrays.asList("deep rag", "deep RAG", "advanced rag", "deep retrieval"));
        
        // Form commands
        addCommandMapping("focus_email", "focus", Arrays.asList("email field", "email input", "email"));
        addCommandMapping("focus_password", "focus", Arrays.asList("password field", "password input", "password"));
        addCommandMapping("focus_name", "focus", Arrays.asList("name field", "name input", "name"));
        
        // Type commands
        addCommandMapping("type_text", "type", Arrays.asList("type", "enter", "input"));
        
        // Clear commands
        addCommandMapping("clear_field", "clear", Arrays.asList("clear", "delete", "remove"));
        addCommandMapping("clear_all", "clear", Arrays.asList("clear all", "reset form", "start over"));
        
        // Scroll commands
        addCommandMapping("scroll_up", "scroll", Arrays.asList("scroll up", "go up"));
        addCommandMapping("scroll_down", "scroll", Arrays.asList("scroll down", "go down"));
        
        // Search commands
        addCommandMapping("search", "search", Arrays.asList("search", "find", "look for"));
    }
    
    /**
     * Get available actions for a specific UI context
     */
    public JSONObject getAvailableActionsForContext(String contextName) {
        UIContext context = contextMappings.get(contextName);
        if (context == null) {
            context = contextMappings.get("form"); // Default to form context
        }
        
        try {
            JSONObject response = new JSONObject();
            response.put("context", contextName);
            response.put("contextTitle", context.getTitle());
            
            JSONArray actions = new JSONArray();
            for (UIAction action : context.getActions()) {
                JSONObject actionObj = new JSONObject();
                actionObj.put("action", action.getAction());
                actionObj.put("target", action.getTarget());
                actionObj.put("label", action.getLabel());
                actionObj.put("description", action.getDescription());
                actions.put(actionObj);
            }
            
            response.put("actions", actions);
            return response;
        } catch (Exception e) {
            System.err.println("❌ Error creating actions JSON: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    /**
     * Execute a UI command based on voice input
     */
    public UICommandResult executeUICommand(String voiceCommand, String currentContext) {
        String normalizedCommand = voiceCommand.toLowerCase().trim();
        
        // Find matching command mapping
        ActionMapping mapping = findBestCommandMapping(normalizedCommand);
        if (mapping == null) {
            return UICommandResult.failure("No matching command found for: " + voiceCommand);
        }
        
        // Get context-specific target
        String target = resolveTarget(mapping, currentContext, normalizedCommand);
        if (target == null) {
            return UICommandResult.failure("Could not determine target for command: " + voiceCommand);
        }
        
        // Extract parameters if needed
        JSONObject parameters = extractParameters(normalizedCommand, mapping);
        
        return UICommandResult.success(mapping.getAction(), target, parameters, currentContext);
    }
    
    /**
     * Find the best matching command mapping for voice input
     */
    private ActionMapping findBestCommandMapping(String voiceCommand) {
        ActionMapping bestMatch = null;
        int bestScore = 0;
        
        for (ActionMapping mapping : commandMappings.values()) {
            for (String pattern : mapping.getVoicePatterns()) {
                int score = calculateMatchScore(voiceCommand, pattern);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = mapping;
                }
            }
        }
        
        return bestScore > 0 ? bestMatch : null;
    }
    
    /**
     * Calculate match score between voice command and pattern
     */
    private int calculateMatchScore(String command, String pattern) {
        if (command.contains(pattern)) {
            return pattern.length(); // Longer matches score higher
        }
        
        // Check for partial word matches
        String[] commandWords = command.split("\\s+");
        String[] patternWords = pattern.split("\\s+");
        
        int matches = 0;
        for (String patternWord : patternWords) {
            for (String commandWord : commandWords) {
                if (commandWord.equals(patternWord)) {
                    matches++;
                    break;
                }
            }
        }
        
        return matches == patternWords.length ? matches * 2 : 0;
    }
    
    /**
     * Resolve target element based on mapping and context
     */
    private String resolveTarget(ActionMapping mapping, String context, String voiceCommand) {
        UIContext uiContext = contextMappings.get(context);
        if (uiContext == null) {
            return null;
        }
        
        // Look for context-specific targets
        for (UIAction action : uiContext.getActions()) {
            if (action.getAction().equals(mapping.getAction())) {
                if (isTargetMatch(action.getTarget(), action.getLabel(), voiceCommand)) {
                    return action.getTarget();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if target matches voice command
     */
    private boolean isTargetMatch(String target, String label, String voiceCommand) {
        String lowerLabel = label.toLowerCase();
        String lowerCommand = voiceCommand.toLowerCase();
        
        // Direct label match
        if (lowerCommand.contains(lowerLabel)) {
            return true;
        }
        
        // Check individual words
        String[] labelWords = lowerLabel.split("\\s+");
        for (String word : labelWords) {
            if (lowerCommand.contains(word) && word.length() > 2) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extract parameters from voice command
     */
    private JSONObject extractParameters(String voiceCommand, ActionMapping mapping) {
        try {
            JSONObject parameters = new JSONObject();
            
            if ("type".equals(mapping.getAction())) {
                // Extract text to type after common phrases
                String textToType = extractTextToType(voiceCommand);
                if (textToType != null) {
                    parameters.put("text", textToType);
                }
            }
            
            return parameters;
        } catch (Exception e) {
            System.err.println("❌ Error extracting parameters: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    /**
     * Extract text to type from voice command
     */
    private String extractTextToType(String voiceCommand) {
        String[] phrases = {"type ", "enter ", "input ", "write "};
        
        for (String phrase : phrases) {
            int index = voiceCommand.toLowerCase().indexOf(phrase);
            if (index >= 0) {
                String remaining = voiceCommand.substring(index + phrase.length()).trim();
                return remaining.isEmpty() ? null : remaining;
            }
        }
        
        return null;
    }
    
    /**
     * Add a command mapping
     */
    private void addCommandMapping(String id, String action, List<String> voicePatterns) {
        commandMappings.put(id, new ActionMapping(id, action, voicePatterns));
    }
    
    /**
     * Register a new UI context
     */
    public void registerUIContext(String contextName, String title) {
        contextMappings.put(contextName, new UIContext(contextName, title));
    }
    
    /**
     * Add action to existing context
     */
    public void addActionToContext(String contextName, String action, String target, String label, String description) {
        UIContext context = contextMappings.get(contextName);
        if (context != null) {
            context.addAction(action, target, label, description);
        }
    }
    
    // Inner classes for data structures
    
    private static class UIContext {
        private final String name;
        private final String title;
        private final List<UIAction> actions = new ArrayList<>();
        
        public UIContext(String name, String title) {
            this.name = name;
            this.title = title;
        }
        
        public void addAction(String action, String target, String label, String description) {
            actions.add(new UIAction(action, target, label, description));
        }
        
        public String getTitle() { return title; }
        public List<UIAction> getActions() { return actions; }
    }
    
    private static class UIAction {
        private final String action;
        private final String target;
        private final String label;
        private final String description;
        
        public UIAction(String action, String target, String label, String description) {
            this.action = action;
            this.target = target;
            this.label = label;
            this.description = description;
        }
        
        public String getAction() { return action; }
        public String getTarget() { return target; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }
    
    private static class ActionMapping {
        private final String id;
        private final String action;
        private final List<String> voicePatterns;
        
        public ActionMapping(String id, String action, List<String> voicePatterns) {
            this.id = id;
            this.action = action;
            this.voicePatterns = voicePatterns;
        }
        
        public String getAction() { return action; }
        public List<String> getVoicePatterns() { return voicePatterns; }
    }
    
    public static class UICommandResult {
        private final boolean success;
        private final String action;
        private final String target;
        private final JSONObject parameters;
        private final String context;
        private final String errorMessage;
        
        private UICommandResult(boolean success, String action, String target, 
                              JSONObject parameters, String context, String errorMessage) {
            this.success = success;
            this.action = action;
            this.target = target;
            this.parameters = parameters;
            this.context = context;
            this.errorMessage = errorMessage;
        }
        
        public static UICommandResult success(String action, String target, JSONObject parameters, String context) {
            return new UICommandResult(true, action, target, parameters, context, null);
        }
        
        public static UICommandResult failure(String errorMessage) {
            return new UICommandResult(false, null, null, null, null, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getAction() { return action; }
        public String getTarget() { return target; }
        public JSONObject getParameters() { return parameters; }
        public String getContext() { return context; }
        public String getErrorMessage() { return errorMessage; }
    }
} 