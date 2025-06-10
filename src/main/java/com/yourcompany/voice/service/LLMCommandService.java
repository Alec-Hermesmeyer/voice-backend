package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import okhttp3.*;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@Service
public class LLMCommandService {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Autowired
    private UIControlService uiControlService;
    
    /**
     * Process natural language command using LLM
     */
    public JSONObject processNaturalLanguageCommand(String command, String currentContext, Map<String, Object> uiState) throws JSONException {
        try {
            // Get available actions for current context
            JSONObject availableActions = uiControlService.getAvailableActionsForContext(currentContext);
            
            // Construct prompt for LLM
            JSONArray messages = new JSONArray()
                .put(new JSONObject()
                    .put("role", "system")
                    .put("content", constructSystemPrompt()))
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", constructUserPrompt(command, currentContext, availableActions, uiState)));
            
            // Call OpenAI API
            JSONObject requestBody = new JSONObject()
                .put("model", "gpt-4-turbo-preview")
                .put("messages", messages)
                .put("temperature", 0.2)
                .put("response_format", new JSONObject().put("type", "json_object"));
            
            Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("OpenAI API error: " + response.code());
                }
                
                JSONObject responseJson = new JSONObject(response.body().string());
                String content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                
                return new JSONObject(content);
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing natural language command: " + e.getMessage());
            return new JSONObject()
                .put("success", false)
                .put("error", e.getMessage());
        }
    }
    
    /**
     * Construct system prompt for LLM
     */
    private String constructSystemPrompt() {
        return "You are a UI control system that converts natural language commands into structured UI actions. " +
               "Your task is to interpret user commands and map them to available UI actions. " +
               "\n\n" +
               "Navigation commands like 'go to [page]' or 'open [page]' should be mapped to the 'navigate' action. " +
               "For navigation, the target should be the URL path (e.g., '/dashboard' for dashboard, '/fast-rag' for fast RAG, '/deep-rag' for deep RAG). " +
               "\n\n" +
               "Special page mappings:\n" +
               "- 'fast rag', 'fast RAG', 'quick rag' → '/fast-rag'\n" +
               "- 'deep rag', 'deep RAG', 'advanced rag' → '/deep-rag'\n" +
               "- 'dashboard', 'main dashboard' → '/dashboard'\n" +
               "- 'settings', 'preferences' → '/settings'\n" +
               "- 'contracts', 'contract page' → '/contracts'\n" +
               "- 'insurance', 'policies' → '/insurance'\n" +
               "- 'records', 'record search' → '/records'\n" +
               "- 'profile', 'my account' → '/profile'\n" +
               "\n\n" +
               "You should return a JSON object with the following structure:\n" +
               "{\n" +
               "  \"success\": boolean,\n" +
               "  \"action\": string,\n" +
               "  \"target\": string,\n" +
               "  \"parameters\": object,\n" +
               "  \"confidence\": number,\n" +
               "  \"feedback\": string\n" +
               "}\n" +
               "\n" +
               "Example navigation commands:\n" +
               "Input: 'go to contracts page'\n" +
               "Output: {\n" +
               "  \"success\": true,\n" +
               "  \"action\": \"navigate\",\n" +
               "  \"target\": \"/contracts\",\n" +
               "  \"parameters\": {},\n" +
               "  \"confidence\": 0.95,\n" +
               "  \"feedback\": \"Navigating to contracts page\"\n" +
               "}\n" +
               "\n" +
               "Input: 'navigate to fast rag'\n" +
               "Output: {\n" +
               "  \"success\": true,\n" +
               "  \"action\": \"navigate\",\n" +
               "  \"target\": \"/fast-rag\",\n" +
               "  \"parameters\": {},\n" +
               "  \"confidence\": 0.95,\n" +
               "  \"feedback\": \"Navigating to fast RAG page\"\n" +
               "}\n" +
               "\n" +
               "If you cannot map the command to an action, return success: false with an error message.";
    }
    
    /**
     * Construct user prompt with context and available actions
     */
    private String constructUserPrompt(String command, String currentContext, 
                                     JSONObject availableActions, Map<String, Object> uiState) throws JSONException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Current context: ").append(currentContext).append("\n\n");
        
        // Always include navigation context
        JSONObject navigationActions = uiControlService.getAvailableActionsForContext("navigation");
        prompt.append("Available navigation:\n").append(navigationActions.toString(2)).append("\n\n");
        
        // Context-specific actions
        prompt.append("Context actions:\n").append(availableActions.toString(2)).append("\n\n");
        
        prompt.append("UI state:\n").append(new JSONObject(uiState).toString(2)).append("\n\n");
        prompt.append("User command: ").append(command).append("\n\n");
        prompt.append("Convert this command into a UI action using the available actions and context.");
        
        return prompt.toString();
    }
} 