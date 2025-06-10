package com.yourcompany.voice.service;

import com.yourcompany.voice.controller.UIControlWebSocketHandler;
import com.yourcompany.voice.service.RAGService.RAGResponse;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Arrays;

@Service
public class CommandService {

    private static final Set<String> COMMAND_WORDS = Set.of(
            "submit", "send", "execute", "go",
            "clear chat", "delete last",
            "enable autospeak", "disable autospeak",
            "show help"
    );

    private static final Map<String, Runnable> COMMAND_ACTIONS = new HashMap<>();
    private final UIControlService uiControlService;
    private final LLMCommandService llmCommandService;
    private final RAGService ragService;

    @Autowired
    public CommandService(UIControlService uiControlService, LLMCommandService llmCommandService, RAGService ragService) {
        this.uiControlService = uiControlService;
        this.llmCommandService = llmCommandService;
        this.ragService = ragService;
        initializeCommandActions();
    }

    private void initializeCommandActions() {
        COMMAND_ACTIONS.put("clear chat", this::clearChat);
        COMMAND_ACTIONS.put("delete last", this::deleteLastMessage);
        COMMAND_ACTIONS.put("enable autospeak", this::enableAutoSpeak);
        COMMAND_ACTIONS.put("disable autospeak", this::disableAutoSpeak);
        COMMAND_ACTIONS.put("show help", this::showHelp);
        COMMAND_ACTIONS.put("submit", () -> executeSubmit("default")); // Submit with optional parameters
        COMMAND_ACTIONS.put("send", () -> executeSubmit("default"));
        COMMAND_ACTIONS.put("execute", () -> executeSubmit("default"));
        COMMAND_ACTIONS.put("go", () -> executeSubmit("default"));
    }

    public Optional<String> detectCommand(String transcript) {
        String cleanedTranscript = transcript.trim().toLowerCase();
        return COMMAND_WORDS.stream()
                .filter(cleanedTranscript::contains)
                .findFirst();
    }

    public String executeCommand(String command) {
        Runnable action = COMMAND_ACTIONS.get(command);
        if (action != null) {
            action.run();
            return command.toUpperCase().replace(" ", "_");
        }
        return "UNKNOWN_COMMAND";
    }

    /**
     * Process voice input and execute UI commands
     */
    public String processVoiceCommand(String voiceInput, String currentContext) throws JSONException {
        return processVoiceCommand(voiceInput, currentContext, null);
    }

    /**
     * Process voice input with optional client ID for RAG context
     */
    public String processVoiceCommand(String voiceInput, String currentContext, String clientId) throws JSONException {
        // First check for traditional commands
        Optional<String> traditionalCommand = detectCommand(voiceInput);
        if (traditionalCommand.isPresent()) {
            return executeCommand(traditionalCommand.get());
        }

        // If client ID is provided, try RAG-enhanced processing first
        if (clientId != null && !clientId.trim().isEmpty()) {
            try {
                RAGResponse ragResponse = ragService.processVoiceCommandWithRAG(clientId, voiceInput, currentContext);
                
                if (ragResponse.isSuccess() && ragResponse.getRelevantDocuments().size() > 0) {
                    // RAG found relevant context, send the enhanced response
                    UIControlWebSocketHandler.broadcastVoiceFeedback(ragResponse.getResponse(), "system");
                    
                    // Try to extract any UI commands from the RAG response
                    String ragResponseText = ragResponse.getResponse().toLowerCase();
                    if (containsUICommand(ragResponseText)) {
                        return processUICommandFromRAGResponse(ragResponseText, currentContext);
                    }
                    
                    return "RAG_COMMAND_EXECUTED";
                }
            } catch (Exception e) {
                System.err.println("❌ Error processing RAG command: " + e.getMessage());
                // Fall back to standard processing
            }
        }

        // Fall back to standard LLM processing
        return processWithStandardLLM(voiceInput, currentContext);
    }

    /**
     * Process voice command using standard LLM without RAG context
     */
    private String processWithStandardLLM(String voiceInput, String currentContext) throws JSONException {
        // Get current UI state
        Map<String, Object> uiState = new HashMap<>();
        uiState.put("currentContext", currentContext);
        uiState.put("availablePages", Arrays.asList(
            "/", "/dashboard", "/contracts", "/insurance", "/records", "/settings", "/profile", "/fast-rag", "/deep-rag"
        ));

        // Process with LLM
        JSONObject llmResult = llmCommandService.processNaturalLanguageCommand(voiceInput, currentContext, uiState);
        
        if (llmResult.getBoolean("success")) {
            // Send UI command via WebSocket
            try {
                String action = llmResult.getString("action");
                String target = llmResult.getString("target");
                JSONObject parameters = llmResult.getJSONObject("parameters");
                
                // Special handling for navigation commands
                if ("navigate".equals(action)) {
                    // Ensure target starts with /
                    if (!target.startsWith("/")) {
                        target = "/" + target;
                    }
                    
                    // Add URL to parameters
                    parameters.put("url", target);
                }
                
                UIControlWebSocketHandler.broadcastUICommand(
                    action,
                    target,
                    parameters,
                    currentContext
                );
                
                // Send voice feedback
                String feedbackText = llmResult.getString("feedback");
                UIControlWebSocketHandler.broadcastVoiceFeedback(feedbackText, "system");
                
                return "LLM_COMMAND_EXECUTED";
            } catch (Exception e) {
                System.err.println("❌ Error executing LLM command: " + e.getMessage());
                return "LLM_COMMAND_FAILED";
            }
        } else {
            String errorMessage = llmResult.optString("error", "Command not understood");
            System.out.println("⚠️ LLM command not recognized: " + errorMessage);
            UIControlWebSocketHandler.broadcastVoiceFeedback(
                errorMessage, 
                "system"
            );
            return "LLM_COMMAND_NOT_RECOGNIZED";
        }
    }

    /**
     * Check if RAG response contains UI commands
     */
    private boolean containsUICommand(String response) {
        return response.contains("navigate") || response.contains("click") || 
               response.contains("open") || response.contains("go to") ||
               response.contains("type") || response.contains("focus") ||
               response.contains("scroll") || response.contains("clear");
    }

    /**
     * Extract and process UI commands from RAG response
     */
    private String processUICommandFromRAGResponse(String ragResponse, String currentContext) {
        try {
            // Use LLM to extract structured commands from RAG response
            Map<String, Object> uiState = new HashMap<>();
            uiState.put("currentContext", currentContext);
            uiState.put("ragResponse", ragResponse);
            
            JSONObject llmResult = llmCommandService.processNaturalLanguageCommand(ragResponse, currentContext, uiState);
            
            if (llmResult.getBoolean("success")) {
                String action = llmResult.getString("action");
                String target = llmResult.getString("target");
                JSONObject parameters = llmResult.getJSONObject("parameters");
                
                UIControlWebSocketHandler.broadcastUICommand(
                    action,
                    target,
                    parameters,
                    currentContext
                );
                
                return "RAG_UI_COMMAND_EXECUTED";
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing UI command from RAG response: " + e.getMessage());
        }
        
        return "RAG_RESPONSE_ONLY";
    }

    /**
     * Generate feedback text for UI commands
     */
    private String generateCommandFeedback(UIControlService.UICommandResult result) {
        switch (result.getAction()) {
            case "click":
                return "Clicked " + result.getTarget();
            case "type":
                JSONObject params = result.getParameters();
                String text = params != null ? params.optString("text", "") : "";
                return "Typed: " + text;
            case "focus":
                return "Focused on " + result.getTarget();
            case "navigate":
                return "Navigating to " + result.getTarget();
            case "scroll":
                return "Scrolling to " + result.getTarget();
            case "clear":
                return "Cleared " + result.getTarget();
            default:
                return "Executed " + result.getAction() + " on " + result.getTarget();
        }
    }

    // ** Actual Actions **

    private void clearChat() {
        System.out.println("🧹 Chat has been cleared!");
        // Backend logic: Notify frontend, update logs, reset storage, etc.
    }

    private void deleteLastMessage() {
        System.out.println("❌ Last message deleted!");
        // Backend logic to remove the last stored message
    }

    private void enableAutoSpeak() {
        System.out.println("🔊 Auto-speak enabled!");
        // Logic to store this state
    }

    private void disableAutoSpeak() {
        System.out.println("🔇 Auto-speak disabled!");
        // Logic to store this state
    }

    private void showHelp() {
        System.out.println("📜 Showing help info...");
        // Potentially return a list of available commands to the frontend
    }

    private void executeSubmit(String parameter) {
        System.out.println("🚀 Submitting with param: " + parameter);
        // Execute actual submission logic here
    }
}
