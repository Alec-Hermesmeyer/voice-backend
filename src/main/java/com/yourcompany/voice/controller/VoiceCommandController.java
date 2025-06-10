package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.CommandService;
import com.yourcompany.voice.service.UIControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;
import org.json.JSONException;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = {"http://localhost:3000", "https://*.vercel.app"})
public class VoiceCommandController {

    @Autowired
    private CommandService commandService;
    
    @Autowired
    private UIControlService uiControlService;

    /**
     * Process a voice command from the frontend
     */
    @PostMapping("/command")
    public ResponseEntity<CommandResponse> processVoiceCommand(@RequestBody VoiceCommandRequest request) {
        try {
            String result = commandService.processVoiceCommand(
                request.getTranscript(), 
                request.getCurrentContext(),
                request.getClientId()
            );
            
            return ResponseEntity.ok(new CommandResponse(
                true,
                result,
                "Command processed successfully",
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error processing voice command: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.ok(new CommandResponse(
                false,
                "ERROR",
                "Failed to process command: " + e.getMessage(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Get available actions for a specific UI context
     */
    @GetMapping("/actions/{context}")
    public ResponseEntity<String> getAvailableActions(@PathVariable String context) {
        try {
            JSONObject actions = uiControlService.getAvailableActionsForContext(context);
            return ResponseEntity.ok(actions.toString());
        } catch (Exception e) {
            System.err.println("❌ Error getting available actions: " + e.getMessage());
            return ResponseEntity.internalServerError().body("{\"error\":\"Failed to get actions\"}");
        }
    }

    /**
     * Register a new UI context with available actions
     */
    @PostMapping("/context")
    public ResponseEntity<String> registerUIContext(@RequestBody UIContextRequest request) {
        try {
            uiControlService.registerUIContext(request.getContextName(), request.getTitle());
            
            if (request.getActions() != null) {
                for (UIActionRequest action : request.getActions()) {
                    uiControlService.addActionToContext(
                        request.getContextName(),
                        action.getAction(),
                        action.getTarget(),
                        action.getLabel(),
                        action.getDescription()
                    );
                }
            }
            
            return ResponseEntity.ok("{\"success\":true,\"message\":\"Context registered successfully\"}");
        } catch (Exception e) {
            System.err.println("❌ Error registering UI context: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("{\"success\":false,\"error\":\"Failed to register context\"}");
        }
    }

    /**
     * Test voice command parsing without execution
     */
    @PostMapping("/test-command")
    public ResponseEntity<TestCommandResponse> testVoiceCommand(@RequestBody VoiceCommandRequest request) {
        try {
            UIControlService.UICommandResult result = uiControlService.executeUICommand(
                request.getTranscript(), 
                request.getCurrentContext()
            );
            
            return ResponseEntity.ok(new TestCommandResponse(
                result.isSuccess(),
                request.getTranscript(),
                result.isSuccess() ? result.getAction() : null,
                result.isSuccess() ? result.getTarget() : null,
                result.isSuccess() ? result.getParameters().toString() : null,
                result.isSuccess() ? null : result.getErrorMessage()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new TestCommandResponse(
                false,
                request.getTranscript(),
                null,
                null,
                null,
                "Error processing command: " + e.getMessage()
            ));
        }
    }

    // Request/Response DTOs
    
    public static class VoiceCommandRequest {
        private String transcript;
        private String currentContext;
        private String sessionId;
        private String clientId;

        // Constructors
        public VoiceCommandRequest() {}
        
        public VoiceCommandRequest(String transcript, String currentContext, String sessionId) {
            this.transcript = transcript;
            this.currentContext = currentContext;
            this.sessionId = sessionId;
        }
        
        public VoiceCommandRequest(String transcript, String currentContext, String sessionId, String clientId) {
            this.transcript = transcript;
            this.currentContext = currentContext;
            this.sessionId = sessionId;
            this.clientId = clientId;
        }

        // Getters and Setters
        public String getTranscript() { return transcript; }
        public void setTranscript(String transcript) { this.transcript = transcript; }
        
        public String getCurrentContext() { return currentContext; }
        public void setCurrentContext(String currentContext) { this.currentContext = currentContext; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
    }

    public static class CommandResponse {
        private boolean success;
        private String result;
        private String message;
        private long timestamp;

        public CommandResponse(boolean success, String result, String message, long timestamp) {
            this.success = success;
            this.result = result;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getResult() { return result; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }

    public static class TestCommandResponse {
        private boolean recognized;
        private String originalCommand;
        private String action;
        private String target;
        private String parameters;
        private String error;

        public TestCommandResponse(boolean recognized, String originalCommand, String action, 
                                 String target, String parameters, String error) {
            this.recognized = recognized;
            this.originalCommand = originalCommand;
            this.action = action;
            this.target = target;
            this.parameters = parameters;
            this.error = error;
        }

        // Getters
        public boolean isRecognized() { return recognized; }
        public String getOriginalCommand() { return originalCommand; }
        public String getAction() { return action; }
        public String getTarget() { return target; }
        public String getParameters() { return parameters; }
        public String getError() { return error; }
    }

    public static class UIContextRequest {
        private String contextName;
        private String title;
        private UIActionRequest[] actions;

        // Getters and Setters
        public String getContextName() { return contextName; }
        public void setContextName(String contextName) { this.contextName = contextName; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public UIActionRequest[] getActions() { return actions; }
        public void setActions(UIActionRequest[] actions) { this.actions = actions; }
    }

    public static class UIActionRequest {
        private String action;
        private String target;
        private String label;
        private String description;

        // Getters and Setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
} 