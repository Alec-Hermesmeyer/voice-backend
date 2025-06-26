package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Speaker identification and voice biometrics service
 * Handles speaker enrollment, identification, and verification
 */
@Service
public class SpeakerIdentificationService {
    
    private static final Logger logger = Logger.getLogger(SpeakerIdentificationService.class.getName());
    private static final String AZURE_SPEAKER_RECOGNITION_URL = "https://westus.api.cognitive.microsoft.com/speaker/recognition/v1.0/";
    
    private final OkHttpClient client = new OkHttpClient();
    
    @Value("${azure.cognitive.api.key:}")
    private String azureCognitiveKey;
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    // Session-based speaker data
    private final Map<String, SessionSpeakerData> sessionSpeakers = new ConcurrentHashMap<>();
    
    // Enrolled speaker profiles (persistent across sessions)
    private final Map<String, SpeakerProfile> enrolledSpeakers = new ConcurrentHashMap<>();
    
    /**
     * Initialize speaker identification for a session
     */
    public void initializeSession(String sessionId, int expectedSpeakers) {
        SessionSpeakerData sessionData = new SessionSpeakerData(sessionId, expectedSpeakers);
        sessionSpeakers.put(sessionId, sessionData);
        
        logger.info("Initialized speaker identification for session: " + sessionId + 
                   " with " + expectedSpeakers + " expected speakers");
    }
    
    /**
     * Identify speaker from audio input
     */
    public SpeakerResult identifySpeaker(String sessionId, byte[] audioData, String transcript) {
        SessionSpeakerData sessionData = sessionSpeakers.get(sessionId);
        if (sessionData == null) {
            return SpeakerResult.error("Session not initialized");
        }
        
        try {
            // Extract voice features from audio
            VoiceFeatures voiceFeatures = extractVoiceFeatures(audioData);
            
            // Try to match against known speakers in this session
            String identifiedSpeaker = matchSpeaker(sessionData, voiceFeatures, transcript);
            
            if (identifiedSpeaker == null) {
                // New speaker detected
                identifiedSpeaker = registerNewSpeaker(sessionData, voiceFeatures, transcript);
            }
            
            // Update speaker activity
            sessionData.updateSpeakerActivity(identifiedSpeaker, voiceFeatures, transcript);
            
            // Calculate confidence score
            double confidence = calculateConfidence(sessionData, identifiedSpeaker, voiceFeatures);
            
            logger.info("Identified speaker: " + identifiedSpeaker + " with confidence: " + confidence);
            
            return SpeakerResult.success(identifiedSpeaker, confidence, voiceFeatures);
            
        } catch (Exception e) {
            logger.severe("Error identifying speaker: " + e.getMessage());
            return SpeakerResult.error("Speaker identification failed: " + e.getMessage());
        }
    }
    
    /**
     * Enroll a speaker for future identification across sessions
     */
    public EnrollmentResult enrollSpeaker(String speakerId, String sessionId, List<byte[]> audioSamples) {
        try {
            // Extract voice features from multiple samples
            List<VoiceFeatures> featuresList = new ArrayList<>();
            for (byte[] audioData : audioSamples) {
                featuresList.add(extractVoiceFeatures(audioData));
            }
            
            // Create speaker profile
            SpeakerProfile profile = createSpeakerProfile(speakerId, featuresList);
            enrolledSpeakers.put(speakerId, profile);
            
            logger.info("Enrolled speaker: " + speakerId);
            return EnrollmentResult.success(profile);
            
        } catch (Exception e) {
            logger.severe("Error enrolling speaker: " + e.getMessage());
            return EnrollmentResult.error("Speaker enrollment failed: " + e.getMessage());
        }
    }
    
    /**
     * Get speaker statistics for a session
     */
    public Map<String, Object> getSpeakerStats(String sessionId) {
        SessionSpeakerData sessionData = sessionSpeakers.get(sessionId);
        if (sessionData == null) {
            return Map.of();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("detectedSpeakers", sessionData.getDetectedSpeakers().size());
        stats.put("expectedSpeakers", sessionData.getExpectedSpeakers());
        stats.put("speakerActivity", sessionData.getSpeakerActivityStats());
        
        return stats;
    }
    
    /**
     * End session and cleanup
     */
    public void endSession(String sessionId) {
        sessionSpeakers.remove(sessionId);
        logger.info("Ended speaker identification session: " + sessionId);
    }
    
    /**
     * Extract voice features using AI analysis
     */
    private VoiceFeatures extractVoiceFeatures(byte[] audioData) throws IOException {
        // For now, we'll use a combination of audio analysis and AI
        // In production, you'd want to use proper voice biometrics
        
        VoiceFeatures features = new VoiceFeatures();
        features.audioLength = audioData.length;
        features.timestamp = System.currentTimeMillis();
        
        // Basic audio characteristics (in production, use proper DSP)
        features.averageVolume = calculateAverageVolume(audioData);
        features.pitch = estimatePitch(audioData);
        features.speakingRate = estimateSpeakingRate(audioData);
        
        // Generate voice fingerprint using AI
        features.voiceFingerprint = generateVoiceFingerprint(audioData);
        
        return features;
    }
    
    /**
     * Generate voice fingerprint using OpenAI or Azure
     */
    private String generateVoiceFingerprint(byte[] audioData) throws IOException {
        // This is a simplified version. In production, you'd use proper voice biometrics
        // or Azure Speaker Recognition API
        
        if (azureCognitiveKey != null && !azureCognitiveKey.isEmpty()) {
            return generateAzureVoiceFingerprint(audioData);
        } else {
            return generateSimpleFingerprint(audioData);
        }
    }
    
    private String generateAzureVoiceFingerprint(byte[] audioData) throws IOException {
        // Use Azure Speaker Recognition API
        // This is a placeholder - implement actual Azure API calls
        return "azure_fingerprint_" + Arrays.hashCode(audioData);
    }
    
    private String generateSimpleFingerprint(byte[] audioData) {
        // Simple hash-based fingerprint
        return "simple_fingerprint_" + Arrays.hashCode(audioData);
    }
    
    /**
     * Match speaker against known speakers in session
     */
    private String matchSpeaker(SessionSpeakerData sessionData, VoiceFeatures features, String transcript) {
        double bestMatch = 0.0;
        String bestSpeaker = null;
        
        for (Map.Entry<String, SpeakerData> entry : sessionData.getDetectedSpeakers().entrySet()) {
            String speakerId = entry.getKey();
            SpeakerData speakerData = entry.getValue();
            
            double similarity = calculateSimilarity(features, speakerData.getAverageFeatures());
            
            if (similarity > bestMatch && similarity > 0.7) { // Threshold for matching
                bestMatch = similarity;
                bestSpeaker = speakerId;
            }
        }
        
        return bestSpeaker;
    }
    
    /**
     * Register new speaker in session
     */
    private String registerNewSpeaker(SessionSpeakerData sessionData, VoiceFeatures features, String transcript) {
        int speakerCount = sessionData.getDetectedSpeakers().size();
        String speakerId = "Speaker_" + (speakerCount + 1);
        
        SpeakerData speakerData = new SpeakerData(speakerId);
        speakerData.addVoiceFeatures(features);
        
        sessionData.addDetectedSpeaker(speakerId, speakerData);
        
        return speakerId;
    }
    
    /**
     * Calculate confidence score for speaker identification
     */
    private double calculateConfidence(SessionSpeakerData sessionData, String speakerId, VoiceFeatures features) {
        SpeakerData speakerData = sessionData.getDetectedSpeakers().get(speakerId);
        if (speakerData == null) {
            return 0.5; // New speaker, moderate confidence
        }
        
        double similarity = calculateSimilarity(features, speakerData.getAverageFeatures());
        
        // Factor in number of previous samples for this speaker
        int sampleCount = speakerData.getSampleCount();
        double sampleBonus = Math.min(0.2, sampleCount * 0.05);
        
        return Math.min(0.99, similarity + sampleBonus);
    }
    
    /**
     * Calculate similarity between voice features
     */
    private double calculateSimilarity(VoiceFeatures features1, VoiceFeatures features2) {
        if (features1 == null || features2 == null) {
            return 0.0;
        }
        
        // Simple similarity calculation (in production, use proper voice biometrics)
        double volumeSimilarity = 1.0 - Math.abs(features1.averageVolume - features2.averageVolume) / 100.0;
        double pitchSimilarity = 1.0 - Math.abs(features1.pitch - features2.pitch) / 1000.0;
        double rateSimilarity = 1.0 - Math.abs(features1.speakingRate - features2.speakingRate) / 10.0;
        
        // Fingerprint similarity (simplified)
        double fingerprintSimilarity = features1.voiceFingerprint.equals(features2.voiceFingerprint) ? 1.0 : 0.3;
        
        return (volumeSimilarity + pitchSimilarity + rateSimilarity + fingerprintSimilarity) / 4.0;
    }
    
    /**
     * Create speaker profile from multiple feature samples
     */
    private SpeakerProfile createSpeakerProfile(String speakerId, List<VoiceFeatures> featuresList) {
        SpeakerProfile profile = new SpeakerProfile(speakerId);
        
        for (VoiceFeatures features : featuresList) {
            profile.addFeatures(features);
        }
        
        return profile;
    }
    
    // Audio analysis helper methods (simplified for demo)
    private double calculateAverageVolume(byte[] audioData) {
        // Simplified volume calculation
        long sum = 0;
        for (byte b : audioData) {
            sum += Math.abs(b);
        }
        return (double) sum / audioData.length;
    }
    
    private double estimatePitch(byte[] audioData) {
        // Simplified pitch estimation
        return 200.0 + (audioData.length % 300);
    }
    
    private double estimateSpeakingRate(byte[] audioData) {
        // Simplified speaking rate estimation
        return 150.0 + (audioData.length % 100) / 10.0;
    }
    
    // Inner classes for data structures
    
    public static class SpeakerResult {
        private final boolean success;
        private final String speakerId;
        private final double confidence;
        private final VoiceFeatures features;
        private final String errorMessage;
        
        private SpeakerResult(boolean success, String speakerId, double confidence, 
                             VoiceFeatures features, String errorMessage) {
            this.success = success;
            this.speakerId = speakerId;
            this.confidence = confidence;
            this.features = features;
            this.errorMessage = errorMessage;
        }
        
        public static SpeakerResult success(String speakerId, double confidence, VoiceFeatures features) {
            return new SpeakerResult(true, speakerId, confidence, features, null);
        }
        
        public static SpeakerResult error(String errorMessage) {
            return new SpeakerResult(false, null, 0.0, null, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getSpeakerId() { return speakerId; }
        public double getConfidence() { return confidence; }
        public VoiceFeatures getFeatures() { return features; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class EnrollmentResult {
        private final boolean success;
        private final SpeakerProfile profile;
        private final String errorMessage;
        
        private EnrollmentResult(boolean success, SpeakerProfile profile, String errorMessage) {
            this.success = success;
            this.profile = profile;
            this.errorMessage = errorMessage;
        }
        
        public static EnrollmentResult success(SpeakerProfile profile) {
            return new EnrollmentResult(true, profile, null);
        }
        
        public static EnrollmentResult error(String errorMessage) {
            return new EnrollmentResult(false, null, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public SpeakerProfile getProfile() { return profile; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class VoiceFeatures {
        public int audioLength;
        public long timestamp;
        public double averageVolume;
        public double pitch;
        public double speakingRate;
        public String voiceFingerprint;
    }
    
    public static class SessionSpeakerData {
        private final String sessionId;
        private final int expectedSpeakers;
        private final Map<String, SpeakerData> detectedSpeakers = new ConcurrentHashMap<>();
        
        public SessionSpeakerData(String sessionId, int expectedSpeakers) {
            this.sessionId = sessionId;
            this.expectedSpeakers = expectedSpeakers;
        }
        
        public void addDetectedSpeaker(String speakerId, SpeakerData speakerData) {
            detectedSpeakers.put(speakerId, speakerData);
        }
        
        public void updateSpeakerActivity(String speakerId, VoiceFeatures features, String transcript) {
            SpeakerData speakerData = detectedSpeakers.get(speakerId);
            if (speakerData != null) {
                speakerData.addVoiceFeatures(features);
                speakerData.addTranscript(transcript);
            }
        }
        
        public Map<String, Object> getSpeakerActivityStats() {
            Map<String, Object> stats = new HashMap<>();
            for (Map.Entry<String, SpeakerData> entry : detectedSpeakers.entrySet()) {
                stats.put(entry.getKey(), entry.getValue().getActivityStats());
            }
            return stats;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public int getExpectedSpeakers() { return expectedSpeakers; }
        public Map<String, SpeakerData> getDetectedSpeakers() { return detectedSpeakers; }
    }
    
    public static class SpeakerData {
        private final String speakerId;
        private final List<VoiceFeatures> voiceFeaturesList = new ArrayList<>();
        private final List<String> transcripts = new ArrayList<>();
        private long firstActivity;
        private long lastActivity;
        
        public SpeakerData(String speakerId) {
            this.speakerId = speakerId;
            this.firstActivity = System.currentTimeMillis();
            this.lastActivity = firstActivity;
        }
        
        public void addVoiceFeatures(VoiceFeatures features) {
            voiceFeaturesList.add(features);
            lastActivity = System.currentTimeMillis();
        }
        
        public void addTranscript(String transcript) {
            transcripts.add(transcript);
        }
        
        public VoiceFeatures getAverageFeatures() {
            if (voiceFeaturesList.isEmpty()) {
                return null;
            }
            
            VoiceFeatures average = new VoiceFeatures();
            average.averageVolume = voiceFeaturesList.stream().mapToDouble(f -> f.averageVolume).average().orElse(0.0);
            average.pitch = voiceFeaturesList.stream().mapToDouble(f -> f.pitch).average().orElse(0.0);
            average.speakingRate = voiceFeaturesList.stream().mapToDouble(f -> f.speakingRate).average().orElse(0.0);
            average.voiceFingerprint = voiceFeaturesList.get(0).voiceFingerprint; // Use first sample's fingerprint
            
            return average;
        }
        
        public Map<String, Object> getActivityStats() {
            return Map.of(
                "sampleCount", voiceFeaturesList.size(),
                "transcriptCount", transcripts.size(),
                "firstActivity", firstActivity,
                "lastActivity", lastActivity
            );
        }
        
        public int getSampleCount() {
            return voiceFeaturesList.size();
        }
        
        // Getters
        public String getSpeakerId() { return speakerId; }
        public List<VoiceFeatures> getVoiceFeaturesList() { return voiceFeaturesList; }
        public List<String> getTranscripts() { return transcripts; }
    }
    
    public static class SpeakerProfile {
        private final String speakerId;
        private final List<VoiceFeatures> enrollmentFeatures = new ArrayList<>();
        private final long enrollmentDate;
        
        public SpeakerProfile(String speakerId) {
            this.speakerId = speakerId;
            this.enrollmentDate = System.currentTimeMillis();
        }
        
        public void addFeatures(VoiceFeatures features) {
            enrollmentFeatures.add(features);
        }
        
        // Getters
        public String getSpeakerId() { return speakerId; }
        public List<VoiceFeatures> getEnrollmentFeatures() { return enrollmentFeatures; }
        public long getEnrollmentDate() { return enrollmentDate; }
    }
}