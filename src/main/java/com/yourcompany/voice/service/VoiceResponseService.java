package com.yourcompany.voice.service;

import com.yourcompany.voice.controller.UIControlWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import java.util.logging.Logger;

/**
 * Handles sending voice responses via WebSocket
 * Replaces static WebSocket dependencies with proper service injection
 */
@Service
public class VoiceResponseService {
    
    private static final Logger logger = Logger.getLogger(VoiceResponseService.class.getName());
    
    @Autowired
    private UIControlWebSocketHandler webSocketHandler;
    
    /**
     * Send TTS audio response to client
     */
    public void sendTTSResponse(String sessionId, String text, byte[] audioData) {
        try {
            AudioProcessingService audioService = new AudioProcessingService();
            AudioProcessingService.TTSResponse ttsResponse = 
                audioService.createTTSResponse(sessionId, text, audioData);
            
            JSONObject responseJson = ttsResponse.toJSON();
            
            // Send via WebSocket
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent TTS response for session: " + sessionId);
            
        } catch (Exception e) {
            logger.severe("Error sending TTS response: " + e.getMessage());
            // Fallback to text response
            sendTextResponse(sessionId, text);
        }
    }
    
    /**
     * Send text-only response to client
     */
    public void sendTextResponse(String sessionId, String text) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "voice_feedback")
                .put("text", text)
                .put("sessionId", sessionId)
                .put("audioData", (String) null); // No audio data
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent text response for session: " + sessionId);
            
        } catch (Exception e) {
            logger.severe("Error sending text response: " + e.getMessage());
        }
    }
    
    /**
     * Send error response to client
     */
    public void sendErrorResponse(String sessionId, String errorMessage) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "voice_error")
                .put("text", errorMessage)
                .put("sessionId", sessionId)
                .put("error", true);
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent error response for session: " + sessionId);
            
        } catch (Exception e) {
            logger.severe("Error sending error response: " + e.getMessage());
        }
    }
    
    /**
     * Send turn management response
     */
    public void sendTurnManagementResponse(String sessionId, String message, String currentSpeaker, String waitingFor) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "turn_management")
                .put("text", message)
                .put("sessionId", sessionId)
                .put("currentSpeaker", currentSpeaker)
                .put("waitingFor", waitingFor);
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent turn management response for session: " + sessionId);
            
        } catch (Exception e) {
            logger.severe("Error sending turn management response: " + e.getMessage());
        }
    }
    
    /**
     * Send session status update
     */
    public void sendSessionStatus(String sessionId, String status, Object metadata) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "session_status")
                .put("sessionId", sessionId)
                .put("status", status);
            
            if (metadata != null) {
                responseJson.put("metadata", metadata);
            }
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent session status for session: " + sessionId + " - " + status);
            
        } catch (Exception e) {
            logger.severe("Error sending session status: " + e.getMessage());
        }
    }
    
    /**
     * Send speaker identification update
     */
    public void sendSpeakerUpdate(String sessionId, String speakerId, double confidence) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "speaker_update")
                .put("sessionId", sessionId)
                .put("speakerId", speakerId)
                .put("confidence", confidence);
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent speaker update for session: " + sessionId + " - " + speakerId);
            
        } catch (Exception e) {
            logger.severe("Error sending speaker update: " + e.getMessage());
        }
    }
    
    /**
     * Send conversation flow update
     */
    public void sendConversationUpdate(String sessionId, String flowState, Object flowData) {
        try {
            JSONObject responseJson = new JSONObject()
                .put("type", "conversation_update")
                .put("sessionId", sessionId)
                .put("flowState", flowState);
            
            if (flowData != null) {
                responseJson.put("flowData", flowData);
            }
            
            webSocketHandler.broadcastVoiceFeedback(responseJson.toString(), sessionId);
            
            logger.info("Sent conversation update for session: " + sessionId + " - " + flowState);
            
        } catch (Exception e) {
            logger.severe("Error sending conversation update: " + e.getMessage());
        }
    }
}