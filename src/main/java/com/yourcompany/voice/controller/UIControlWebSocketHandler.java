package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.UIControlService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UIControlWebSocketHandler extends TextWebSocketHandler {
    
    private static final Map<String, WebSocketSession> uiSessions = new ConcurrentHashMap<>();
    private final UIControlService uiControlService;

    public UIControlWebSocketHandler(UIControlService uiControlService) {
        this.uiControlService = uiControlService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        uiSessions.put(session.getId(), session);
        System.out.println("🖥️ UI Control WebSocket connected: " + session.getId() + 
                " (Total UI connections: " + uiSessions.size() + ")");

        try {
            JSONObject welcomeMsg = new JSONObject();
            welcomeMsg.put("type", "ui_control_ready");
            welcomeMsg.put("message", "UI Control connection established");
            welcomeMsg.put("timestamp", System.currentTimeMillis());
            
            session.sendMessage(new TextMessage(welcomeMsg.toString()));
        } catch (IOException | JSONException e) {
            System.err.println("❌ Error sending UI control welcome message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        uiSessions.remove(session.getId());
        System.out.println("🖥️ UI Control WebSocket disconnected: " + session.getId() + 
                " (Total UI connections: " + uiSessions.size() + ")");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject payload = new JSONObject(message.getPayload());
            String messageType = payload.getString("type");
            
            System.out.println("📨 UI Control message from " + session.getId() + ": " + messageType);

            switch (messageType) {
                case "ui_state_update":
                    handleUIStateUpdate(session, payload);
                    break;
                case "action_completed":
                    handleActionCompleted(session, payload);
                    break;
                case "action_failed":
                    handleActionFailed(session, payload);
                    break;
                case "get_available_actions":
                    handleGetAvailableActions(session, payload);
                    break;
                default:
                    System.out.println("⚠️ Unknown UI control message type: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling UI control message: " + e.getMessage());
        }
    }

    private void handleUIStateUpdate(WebSocketSession session, JSONObject payload) {
        String currentPage = payload.optString("currentPage", "unknown");
        String focusedElement = payload.optString("focusedElement", "");
        JSONObject availableElements = payload.optJSONObject("availableElements");
        
        // Store UI state for this session
        session.getAttributes().put("currentPage", currentPage);
        session.getAttributes().put("focusedElement", focusedElement);
        session.getAttributes().put("availableElements", availableElements);
        
        System.out.println("🖥️ UI State updated - Page: " + currentPage + ", Focus: " + focusedElement);
    }

    private void handleActionCompleted(WebSocketSession session, JSONObject payload) {
        String actionId = payload.optString("actionId", "");
        String result = payload.optString("result", "");
        
        System.out.println("✅ UI Action completed - ID: " + actionId + ", Result: " + result);
        
        // Optionally send voice feedback
        broadcastVoiceFeedback("Action completed successfully", session.getId());
    }

    private void handleActionFailed(WebSocketSession session, JSONObject payload) {
        String actionId = payload.optString("actionId", "");
        String error = payload.optString("error", "Unknown error");
        
        System.err.println("❌ UI Action failed - ID: " + actionId + ", Error: " + error);
        
        // Send voice feedback about failure
        broadcastVoiceFeedback("Action failed: " + error, session.getId());
    }

    private void handleGetAvailableActions(WebSocketSession session, JSONObject payload) {
        String currentPage = (String) session.getAttributes().get("currentPage");
        JSONObject availableActions = uiControlService.getAvailableActionsForContext(currentPage);
        
        try {
            JSONObject response = new JSONObject();
            response.put("type", "available_actions");
            response.put("actions", availableActions);
            response.put("timestamp", System.currentTimeMillis());
            
            session.sendMessage(new TextMessage(response.toString()));
        } catch (IOException | JSONException e) {
            System.err.println("❌ Error sending available actions: " + e.getMessage());
        }
    }

    /**
     * Send a UI command to a specific session
     */
    public static void sendUICommand(String sessionId, String action, String target, 
                                   JSONObject parameters, String context) {
        WebSocketSession session = uiSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                JSONObject command = new JSONObject();
                command.put("type", "ui_command");
                command.put("action", action);
                command.put("target", target);
                command.put("parameters", parameters != null ? parameters : new JSONObject());
                command.put("context", context);
                command.put("actionId", generateActionId());
                command.put("timestamp", System.currentTimeMillis());
                
                session.sendMessage(new TextMessage(command.toString()));
                System.out.println("🎯 Sent UI command: " + action + " -> " + target + " (Session: " + sessionId + ")");
                
            } catch (IOException | JSONException e) {
                System.err.println("❌ Error sending UI command: " + e.getMessage());
            }
        } else {
            System.err.println("❌ No active UI session found for ID: " + sessionId);
        }
    }

    /**
     * Broadcast UI command to all connected sessions
     */
    public static void broadcastUICommand(String action, String target, JSONObject parameters, String context) {
        try {
            JSONObject command = new JSONObject();
            command.put("type", "ui_command");
            command.put("action", action);
            command.put("target", target);
            command.put("parameters", parameters != null ? parameters : new JSONObject());
            command.put("context", context);
            command.put("actionId", generateActionId());
            command.put("timestamp", System.currentTimeMillis());
            
            TextMessage message = new TextMessage(command.toString());
            
            for (WebSocketSession session : uiSessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    System.err.println("❌ Error broadcasting UI command: " + e.getMessage());
                }
            }
            
            System.out.println("📢 Broadcasted UI command: " + action + " -> " + target);
        } catch (JSONException e) {
            System.err.println("❌ Error creating broadcast command JSON: " + e.getMessage());
        }
    }

    /**
     * Send voice feedback message
     */
    public static void broadcastVoiceFeedback(String feedbackText, String sessionId) {
        try {
            JSONObject feedback = new JSONObject();
            feedback.put("type", "voice_feedback");
            feedback.put("text", feedbackText);
            feedback.put("sessionId", sessionId);
            feedback.put("timestamp", System.currentTimeMillis());
            
            // This will trigger TTS on the frontend
            TextMessage message = new TextMessage(feedback.toString());
            
            for (WebSocketSession session : uiSessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    System.err.println("❌ Error sending voice feedback: " + e.getMessage());
                }
            }
        } catch (JSONException e) {
            System.err.println("❌ Error creating voice feedback JSON: " + e.getMessage());
        }
    }

    private static String generateActionId() {
        return "action_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
} 