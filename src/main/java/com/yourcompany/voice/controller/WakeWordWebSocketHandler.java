package com.yourcompany.voice.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component  // <--- Add this annotation
public class WakeWordWebSocketHandler extends TextWebSocketHandler {
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("🌐 New WebSocket connection: " + session.getId());
        session.sendMessage(new TextMessage("✅ WebSocket Connected"));

        new Thread(() -> {
            try {
                while (session.isOpen()) {
                    session.sendMessage(new TextMessage("PING"));
                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                System.out.println("🔌 WebSocket connection lost: " + session.getId());
            }
        }).start();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("📩 Received message from frontend: " + message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("🔌 WebSocket closed: " + session.getId());
    }

    public static void broadcastWakeWord(String wakeWord) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    System.out.println("📢 Broadcasting wake word: " + wakeWord);
                    session.sendMessage(new TextMessage("Wake word detected: " + wakeWord));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
