package com.yourcompany.voice.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WakeWordWebSocketHandler extends TextWebSocketHandler {
    // Store all active WebSocket sessions
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Save the session when a new connection is established
        sessions.put(session.getId(), session);
        System.out.println("👋 New WebSocket connection: " + session.getId() +
                " (Total active connections: " + sessions.size() + ")");

        try {
            // Send a welcome message
            String welcomeMsg = "{\"type\":\"connected\",\"message\":\"WebSocket connection established\"}";
            session.sendMessage(new TextMessage(welcomeMsg));
        } catch (IOException e) {
            System.err.println("❌ Error sending welcome message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Remove the session when connection is closed
        sessions.remove(session.getId());
        System.out.println("👋 WebSocket connection closed: " + session.getId() +
                " (Total active connections: " + sessions.size() + ")");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle incoming messages (if needed)
        String payload = message.getPayload();
        System.out.println("📨 Received message from " + session.getId() + ": " + payload);

        // Echo back the message (for testing purposes)
        try {
            String response = "{\"type\":\"echo\",\"message\":\"" + payload.replace("\"", "\\\"") + "\"}";
            session.sendMessage(new TextMessage(response));
        } catch (IOException e) {
            System.err.println("❌ Error sending response: " + e.getMessage());
        }
    }

    /**
     * Broadcast a wake word detection event to all connected clients
     */
    public static void broadcastWakeWordDetection(String wakeWord, String agentId, double confidence) {
        String message = String.format(
                "{\"type\":\"wake_word_detected\",\"wakeWord\":\"%s\",\"agentId\":\"%s\",\"confidence\":%.2f,\"timestamp\":%d}",
                wakeWord, agentId, confidence, System.currentTimeMillis()
        );

        broadcast(message);
    }

    /**
     * Broadcast a wake word training success message
     */
    public static void broadcastWakeWordTrained(String wakeWord) {
        String message = String.format(
                "{\"type\":\"wake_word_trained\",\"wakeWord\":\"%s\",\"timestamp\":%d}",
                wakeWord, System.currentTimeMillis()
        );

        broadcast(message);
    }

    /**
     * Broadcast an error message
     */
    public static void broadcastError(String errorMessage) {
        System.err.println("❌ Broadcasted error: " + errorMessage);

        String message = String.format(
                "{\"type\":\"error\",\"message\":\"%s\",\"timestamp\":%d}",
                errorMessage.replace("\"", "\\\""), System.currentTimeMillis()
        );

        broadcast(message);
    }

    /**
     * Broadcast a message to all connected clients
     */
    private static void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);

        // Send to all active sessions
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                System.err.println("❌ Error sending message to " + session.getId() + ": " + e.getMessage());
            }
        }
    }
}