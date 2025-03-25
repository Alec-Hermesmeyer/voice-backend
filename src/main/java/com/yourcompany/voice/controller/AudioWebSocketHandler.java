package com.yourcompany.voice.controller;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import okio.ByteString;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {
    private static final String DEEPGRAM_WS_URL =
            "wss://api.deepgram.com/v1/listen?encoding=linear16&sample_rate=16000&model=nova&smart_format=true&diarize=true";

    private static final String OPENAI_WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private WebSocket deepgramWebSocket;
    private final OkHttpClient client = new OkHttpClient();

    @Value("${deepgram.api.key}")
    private String deepgramApiKey;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final Set<WebSocketSession> activeSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        activeSessions.add(session);

        String selectedSTT = (String) session.getAttributes().getOrDefault("stt_engine", "deepgram");
        session.getAttributes().put("selectedSTT", selectedSTT);

        System.out.println("🎧 Selected STT Engine: " + selectedSTT);

        if ("whisper".equalsIgnoreCase(selectedSTT)) {
            System.out.println("🗣️ Using Whisper for STT");
        } else {
            connectToDeepgram(session);
        }
    }





    private String getSTTParameter(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("stt_engine=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("stt_engine=")) {
                    return param.split("=")[1];
                }
            }
        }
        return "deepgram"; // Default to Deepgram if no parameter is set
    }

    private void connectToDeepgram(WebSocketSession session) {
        if (deepgramApiKey == null || deepgramApiKey.isEmpty()) {
            System.err.println("❌ Deepgram API key is missing!");
            return;
        }

        Request request = new Request.Builder()
                .url(DEEPGRAM_WS_URL)
                .addHeader("Authorization", "Token " + deepgramApiKey)
                .build();

        deepgramWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("✅ Connected to Deepgram!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("📝 Deepgram Transcription: " + text);
                sendToFrontend(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("❌ Deepgram WebSocket error: " + t.getMessage());
            }
        });
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        String selectedSTT = (String) session.getAttributes().get("selectedSTT");

        if ("whisper".equalsIgnoreCase(selectedSTT)) {
            System.out.println("⚠️ Whisper selected but WebSocket streaming is not supported. Use REST.");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Whisper does not support streaming."));
        } else {
            if (deepgramWebSocket != null) {
                deepgramWebSocket.send(ByteString.of(message.getPayload().array()));
            }
        }
    }




    private void processWithWhisper(WebSocketSession session, BinaryMessage message) {
        System.out.println("🎤 Sending audio to OpenAI Whisper...");

        new Thread(() -> {
            try {
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "audio.wav",
                                RequestBody.create(message.getPayload().array(), MediaType.parse("audio/wav")))
                        .addFormDataPart("model", "whisper-1")
                        .addFormDataPart("response_format", "json")
                        .addFormDataPart("language", "en")
                        .build();

                Request request = new Request.Builder()
                        .url(OPENAI_WHISPER_API_URL)
                        .addHeader("Authorization", "Bearer " + openAiApiKey)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    System.err.println("❌ Whisper API Error: " + response.code() + " " + response.body().string());
                    return;
                }


                String transcription = response.body().string();
                System.out.println("📝 Whisper Transcription: " + transcription);
                sendToFrontend(transcription);

            } catch (Exception e) {
                System.err.println("❌ Failed to process with Whisper: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void sendToFrontend(String transcription) {
        for (WebSocketSession activeSession : activeSessions) {
            if (activeSession.isOpen()) {
                try {
                    activeSession.sendMessage(new TextMessage(transcription));
                    System.out.println("📤 Sent transcription to frontend: " + transcription);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send transcription: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("🔌 Client disconnected: " + session.getId());
        activeSessions.remove(session);
        if (deepgramWebSocket != null) deepgramWebSocket.close(1000, "Client disconnected");
    }
}
