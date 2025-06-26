package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import okhttp3.*;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Handles audio processing including Text-to-Speech and Speech-to-Text
 * Focused responsibility: Audio conversion and caching
 */
@Service
public class AudioProcessingService {
    
    private static final Logger logger = Logger.getLogger(AudioProcessingService.class.getName());
    private static final String OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech";
    private static final String OPENAI_STT_URL = "https://api.openai.com/v1/audio/transcriptions";
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Value("${deepgram.api.key:}")
    private String deepgramApiKey;
    
    /**
     * Convert text to speech asynchronously
     */
    public CompletableFuture<TTSResult> generateTTS(String text, String voiceModel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] audioData = generateTTSSync(text, voiceModel);
                return TTSResult.success(audioData, text, voiceModel);
                
            } catch (Exception e) {
                logger.severe("TTS generation failed: " + e.getMessage());
                return TTSResult.error("TTS generation failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Synchronous TTS generation
     */
    private byte[] generateTTSSync(String text, String voiceModel) throws IOException {
        try {
            JSONObject requestBody = new JSONObject()
                .put("model", "tts-1")
                .put("input", text)
                .put("voice", voiceModel);
            
            Request request = new Request.Builder()
                .url(OPENAI_TTS_URL)
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.severe("OpenAI TTS Error " + response.code() + ": " + errorBody);
                    throw new IOException("TTS API error: " + response.code() + " - " + errorBody);
                }
                return response.body().bytes();
            }
        } catch (Exception e) {
            throw new IOException("Error creating TTS request", e);
        }
    }
    
    /**
     * Convert speech to text using OpenAI Whisper
     */
    public CompletableFuture<STTResult> transcribeAudio(byte[] audioData, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String transcript = transcribeWithWhisper(audioData, language);
                return STTResult.success(transcript, audioData.length);
                
            } catch (Exception e) {
                logger.severe("STT transcription failed: " + e.getMessage());
                
                // Fallback to Deepgram if available
                if (deepgramApiKey != null && !deepgramApiKey.isEmpty()) {
                    try {
                        String transcript = transcribeWithDeepgram(audioData, language);
                        return STTResult.success(transcript, audioData.length);
                    } catch (Exception deepgramError) {
                        logger.severe("Deepgram fallback also failed: " + deepgramError.getMessage());
                    }
                }
                
                return STTResult.error("STT transcription failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Transcribe using OpenAI Whisper
     */
    private String transcribeWithWhisper(byte[] audioData, String language) throws IOException {
        try {
            // Create multipart request body
            RequestBody audioBody = RequestBody.create(audioData, MediaType.parse("audio/wav"));
            
            MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "audio.wav", audioBody)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", language != null ? language : "en")
                .addFormDataPart("response_format", "json")
                .build();
            
            Request request = new Request.Builder()
                .url(OPENAI_STT_URL)
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .post(requestBody)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.severe("OpenAI STT Error " + response.code() + ": " + errorBody);
                    throw new IOException("STT API error: " + response.code() + " - " + errorBody);
                }
                
                JSONObject responseJson = new JSONObject(response.body().string());
                return responseJson.getString("text");
            }
        } catch (Exception e) {
            throw new IOException("Error creating STT request", e);
        }
    }
    
    /**
     * Transcribe using Deepgram (fallback)
     */
    private String transcribeWithDeepgram(byte[] audioData, String language) throws IOException {
        // Deepgram API implementation
        // This is a placeholder - implement actual Deepgram API calls
        throw new IOException("Deepgram transcription not yet implemented");
    }
    
    /**
     * Create TTS response object for WebSocket transmission
     */
    public TTSResponse createTTSResponse(String sessionId, String text, byte[] audioData) {
        try {
            String encodedAudio = Base64.getEncoder().encodeToString(audioData);
            return new TTSResponse(sessionId, text, encodedAudio, "tts_response");
        } catch (Exception e) {
            logger.severe("Error creating TTS response: " + e.getMessage());
            return new TTSResponse(sessionId, text, null, "tts_error");
        }
    }
    
    /**
     * Validate audio format and quality
     */
    public AudioValidationResult validateAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return AudioValidationResult.invalid("Audio data is empty");
        }
        
        if (audioData.length < 1000) { // Minimum 1KB
            return AudioValidationResult.invalid("Audio data too short");
        }
        
        if (audioData.length > 25 * 1024 * 1024) { // Maximum 25MB
            return AudioValidationResult.invalid("Audio data too large");
        }
        
        // Additional format validation could be added here
        return AudioValidationResult.valid();
    }
    
    /**
     * Extract audio metadata
     */
    public AudioMetadata extractAudioMetadata(byte[] audioData) {
        AudioMetadata metadata = new AudioMetadata();
        metadata.setSize(audioData.length);
        metadata.setFormat("wav"); // Default format
        metadata.setTimestamp(System.currentTimeMillis());
        
        // In production, use proper audio analysis libraries
        metadata.setEstimatedDuration(estimateDuration(audioData));
        
        return metadata;
    }
    
    private double estimateDuration(byte[] audioData) {
        // Simplified duration estimation
        // In production, parse actual audio headers
        return (double) audioData.length / 16000.0; // Assume 16kHz mono
    }
    
    // Result classes
    
    public static class TTSResult {
        private final boolean success;
        private final byte[] audioData;
        private final String text;
        private final String voiceModel;
        private final String errorMessage;
        
        private TTSResult(boolean success, byte[] audioData, String text, String voiceModel, String errorMessage) {
            this.success = success;
            this.audioData = audioData;
            this.text = text;
            this.voiceModel = voiceModel;
            this.errorMessage = errorMessage;
        }
        
        public static TTSResult success(byte[] audioData, String text, String voiceModel) {
            return new TTSResult(true, audioData, text, voiceModel, null);
        }
        
        public static TTSResult error(String errorMessage) {
            return new TTSResult(false, null, null, null, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public byte[] getAudioData() { return audioData; }
        public String getText() { return text; }
        public String getVoiceModel() { return voiceModel; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class STTResult {
        private final boolean success;
        private final String transcript;
        private final int audioSize;
        private final String errorMessage;
        
        private STTResult(boolean success, String transcript, int audioSize, String errorMessage) {
            this.success = success;
            this.transcript = transcript;
            this.audioSize = audioSize;
            this.errorMessage = errorMessage;
        }
        
        public static STTResult success(String transcript, int audioSize) {
            return new STTResult(true, transcript, audioSize, null);
        }
        
        public static STTResult error(String errorMessage) {
            return new STTResult(false, null, 0, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getTranscript() { return transcript; }
        public int getAudioSize() { return audioSize; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class TTSResponse {
        private final String sessionId;
        private final String text;
        private final String audioData; // Base64 encoded
        private final String type;
        
        public TTSResponse(String sessionId, String text, String audioData, String type) {
            this.sessionId = sessionId;
            this.text = text;
            this.audioData = audioData;
            this.type = type;
        }
        
        public JSONObject toJSON() {
            try {
                JSONObject json = new JSONObject()
                    .put("type", type)
                    .put("sessionId", sessionId)
                    .put("text", text);
                
                if (audioData != null) {
                    json.put("audioData", audioData);
                }
                
                return json;
            } catch (Exception e) {
                JSONObject errorJson = new JSONObject();
                try {
                    errorJson.put("type", "tts_error");
                    errorJson.put("error", e.getMessage());
                } catch (Exception jsonError) {
                    // Fallback
                }
                return errorJson;
            }
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getText() { return text; }
        public String getAudioData() { return audioData; }
        public String getType() { return type; }
    }
    
    public static class AudioValidationResult {
        private final boolean valid;
        private final String message;
        
        private AudioValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static AudioValidationResult valid() {
            return new AudioValidationResult(true, null);
        }
        
        public static AudioValidationResult invalid(String message) {
            return new AudioValidationResult(false, message);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
    
    public static class AudioMetadata {
        private int size;
        private String format;
        private long timestamp;
        private double estimatedDuration;
        
        // Getters and setters
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public double getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(double estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    }
}