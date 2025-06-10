
package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.VoiceInteractionService;
import com.yourcompany.voice.service.VoiceInteractionService.VoiceSessionConfig;
import com.yourcompany.voice.service.VoiceInteractionService.VoiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Voice Interaction Management
 * Handles complete voice-only user sessions
 */
@RestController
@RequestMapping("/api/voice-interaction")
@CrossOrigin(origins = {"http://localhost:3000", "https://*.vercel.app"})
public class VoiceInteractionController {

    @Autowired
    private VoiceInteractionService voiceInteractionService;

    /**
     * Start a new voice interaction session
     */
    @PostMapping("/sessions/start")
    public ResponseEntity<StartSessionResponse> startSession(@RequestBody StartSessionRequest request) {
        try {
            VoiceSessionConfig config = new VoiceSessionConfig();
            
            // Configure session based on request
            if (request.getTtsEnabled() != null) {
                config.setTtsEnabled(request.getTtsEnabled());
            }
            if (request.getVoiceModel() != null) {
                config.setVoiceModel(request.getVoiceModel());
            }
            if (request.getWelcomeMessage() != null) {
                config.setWelcomeMessage(request.getWelcomeMessage());
            }
            
            String result = voiceInteractionService.startVoiceSession(
                request.getSessionId(), 
                request.getClientId(), 
                config
            );
            
            return ResponseEntity.ok(new StartSessionResponse(
                true,
                result,
                request.getSessionId(),
                request.getClientId(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new StartSessionResponse(
                false,
                "Failed to start session: " + e.getMessage(),
                request.getSessionId(),
                request.getClientId(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Process voice input for a session
     */
    @PostMapping("/sessions/{sessionId}/input")
    public CompletableFuture<ResponseEntity<VoiceInputResponse>> processVoiceInput(
            @PathVariable String sessionId,
            @RequestBody VoiceInputRequest request) {
        
        return voiceInteractionService.processVoiceInput(
            sessionId, 
            request.getTranscript(), 
            request.getCurrentContext()
        ).thenApply(voiceResponse -> {
            return ResponseEntity.ok(new VoiceInputResponse(
                voiceResponse.isSuccess(),
                voiceResponse.getResponseText(),
                voiceResponse.getType().toString(),
                request.getTranscript(),
                request.getCurrentContext(),
                System.currentTimeMillis()
            ));
        }).exceptionally(throwable -> {
            return ResponseEntity.ok(new VoiceInputResponse(
                false,
                "Error processing voice input: " + throwable.getMessage(),
                "ERROR",
                request.getTranscript(),
                request.getCurrentContext(),
                System.currentTimeMillis()
            ));
        });
    }

    /**
     * Send text-to-speech message to user
     */
    @PostMapping("/sessions/{sessionId}/speak")
    public ResponseEntity<SpeakResponse> speakToUser(
            @PathVariable String sessionId,
            @RequestBody SpeakRequest request) {
        
        try {
            voiceInteractionService.speakToUser(sessionId, request.getText());
            
            return ResponseEntity.ok(new SpeakResponse(
                true,
                "Message sent to user",
                request.getText(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new SpeakResponse(
                false,
                "Failed to send message: " + e.getMessage(),
                request.getText(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Get session statistics
     */
    @GetMapping("/sessions/{sessionId}/stats")
    public ResponseEntity<SessionStatsResponse> getSessionStats(@PathVariable String sessionId) {
        try {
            Map<String, Object> stats = voiceInteractionService.getSessionStats(sessionId);
            
            if (stats != null) {
                return ResponseEntity.ok(new SessionStatsResponse(
                    true,
                    "Session statistics retrieved",
                    stats,
                    System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.ok(new SessionStatsResponse(
                    false,
                    "Session not found",
                    null,
                    System.currentTimeMillis()
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.ok(new SessionStatsResponse(
                false,
                "Error retrieving session stats: " + e.getMessage(),
                null,
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * End a voice interaction session
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<EndSessionResponse> endSession(@PathVariable String sessionId) {
        try {
            voiceInteractionService.endVoiceSession(sessionId);
            
            return ResponseEntity.ok(new EndSessionResponse(
                true,
                "Session ended successfully",
                sessionId,
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new EndSessionResponse(
                false,
                "Failed to end session: " + e.getMessage(),
                sessionId,
                System.currentTimeMillis()
            ));
        }
    }

    // Health endpoint moved to separate HealthController to avoid dependency issues

    // Request/Response DTOs

    public static class StartSessionRequest {
        private String sessionId;
        private String clientId;
        private Boolean ttsEnabled;
        private String voiceModel;
        private String welcomeMessage;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Boolean getTtsEnabled() { return ttsEnabled; }
        public void setTtsEnabled(Boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
        public String getVoiceModel() { return voiceModel; }
        public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
        public String getWelcomeMessage() { return welcomeMessage; }
        public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    }

    public static class VoiceInputRequest {
        private String transcript;
        private String currentContext;

        public String getTranscript() { return transcript; }
        public void setTranscript(String transcript) { this.transcript = transcript; }
        public String getCurrentContext() { return currentContext; }
        public void setCurrentContext(String currentContext) { this.currentContext = currentContext; }
    }

    public static class SpeakRequest {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class StartSessionResponse {
        private final boolean success;
        private final String message;
        private final String sessionId;
        private final String clientId;
        private final long timestamp;

        public StartSessionResponse(boolean success, String message, String sessionId, String clientId, long timestamp) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.clientId = clientId;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionId() { return sessionId; }
        public String getClientId() { return clientId; }
        public long getTimestamp() { return timestamp; }
    }

    public static class VoiceInputResponse {
        private final boolean success;
        private final String response;
        private final String responseType;
        private final String originalTranscript;
        private final String context;
        private final long timestamp;

        public VoiceInputResponse(boolean success, String response, String responseType, 
                                String originalTranscript, String context, long timestamp) {
            this.success = success;
            this.response = response;
            this.responseType = responseType;
            this.originalTranscript = originalTranscript;
            this.context = context;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getResponse() { return response; }
        public String getResponseType() { return responseType; }
        public String getOriginalTranscript() { return originalTranscript; }
        public String getContext() { return context; }
        public long getTimestamp() { return timestamp; }
    }

    public static class SpeakResponse {
        private final boolean success;
        private final String message;
        private final String text;
        private final long timestamp;

        public SpeakResponse(boolean success, String message, String text, long timestamp) {
            this.success = success;
            this.message = message;
            this.text = text;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getText() { return text; }
        public long getTimestamp() { return timestamp; }
    }

    public static class SessionStatsResponse {
        private final boolean success;
        private final String message;
        private final Map<String, Object> stats;
        private final long timestamp;

        public SessionStatsResponse(boolean success, String message, Map<String, Object> stats, long timestamp) {
            this.success = success;
            this.message = message;
            this.stats = stats;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getStats() { return stats; }
        public long getTimestamp() { return timestamp; }
    }

    public static class EndSessionResponse {
        private final boolean success;
        private final String message;
        private final String sessionId;
        private final long timestamp;

        public EndSessionResponse(boolean success, String message, String sessionId, long timestamp) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionId() { return sessionId; }
        public long getTimestamp() { return timestamp; }
    }

    public static class HealthResponse {
        private final boolean healthy;
        private final String message;
        private final long timestamp;

        public HealthResponse(boolean healthy, String message, long timestamp) {
            this.healthy = healthy;
            this.message = message;
            this.timestamp = timestamp;
        }

        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
} 