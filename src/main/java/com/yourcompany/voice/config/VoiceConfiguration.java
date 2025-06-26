package com.yourcompany.voice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

/**
 * Centralized configuration for the voice system
 * Replaces scattered configuration constants with organized property management
 */
@Configuration
@ConfigurationProperties(prefix = "voice")
public class VoiceConfiguration {
    
    // API Configuration
    private ApiConfig api = new ApiConfig();
    
    // Audio Configuration
    private AudioConfig audio = new AudioConfig();
    
    // Session Configuration
    private SessionConfig session = new SessionConfig();
    
    // Speaker Identification Configuration
    private SpeakerConfig speaker = new SpeakerConfig();
    
    // Turn Management Configuration
    private ConversationConfig conversation = new ConversationConfig();
    
    // Error Handling Configuration
    private ErrorConfig error = new ErrorConfig();
    
    // Performance Configuration
    private PerformanceConfig performance = new PerformanceConfig();
    
    // Security Configuration
    private SecurityConfig security = new SecurityConfig();
    
    // Getters and setters
    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }
    public AudioConfig getAudio() { return audio; }
    public void setAudio(AudioConfig audio) { this.audio = audio; }
    public SessionConfig getSession() { return session; }
    public void setSession(SessionConfig session) { this.session = session; }
    public SpeakerConfig getSpeaker() { return speaker; }
    public void setSpeaker(SpeakerConfig speaker) { this.speaker = speaker; }
    public ConversationConfig getConversation() { return conversation; }
    public void setConversation(ConversationConfig conversation) { this.conversation = conversation; }
    public ErrorConfig getError() { return error; }
    public void setError(ErrorConfig error) { this.error = error; }
    public PerformanceConfig getPerformance() { return performance; }
    public void setPerformance(PerformanceConfig performance) { this.performance = performance; }
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    
    /**
     * API configuration for external services
     */
    public static class ApiConfig {
        
        private String openaiKey;
        
        private String deepgramKey;
        
        private String azureCognitiveKey;
        
        private String picovoiceKey;
        
        // API URLs
        private String openaiBaseUrl = "https://api.openai.com/v1";
        private String deepgramBaseUrl = "https://api.deepgram.com/v1";
        private String azureBaseUrl = "https://westus.api.cognitive.microsoft.com";
        
        // Timeout settings
        
        
        private int connectTimeoutMs = 10000;
        
        
        
        private int readTimeoutMs = 30000;
        
        
        
        private int writeTimeoutMs = 30000;
        
        // Rate limiting
        
        private int maxRequestsPerMinute = 60;
        
        // Getters and setters
        public String getOpenaiKey() { return openaiKey; }
        public void setOpenaiKey(String openaiKey) { this.openaiKey = openaiKey; }
        public String getDeepgramKey() { return deepgramKey; }
        public void setDeepgramKey(String deepgramKey) { this.deepgramKey = deepgramKey; }
        public String getAzureCognitiveKey() { return azureCognitiveKey; }
        public void setAzureCognitiveKey(String azureCognitiveKey) { this.azureCognitiveKey = azureCognitiveKey; }
        public String getPicovoiceKey() { return picovoiceKey; }
        public void setPicovoiceKey(String picovoiceKey) { this.picovoiceKey = picovoiceKey; }
        public String getOpenaiBaseUrl() { return openaiBaseUrl; }
        public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }
        public String getDeepgramBaseUrl() { return deepgramBaseUrl; }
        public void setDeepgramBaseUrl(String deepgramBaseUrl) { this.deepgramBaseUrl = deepgramBaseUrl; }
        public String getAzureBaseUrl() { return azureBaseUrl; }
        public void setAzureBaseUrl(String azureBaseUrl) { this.azureBaseUrl = azureBaseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public int getWriteTimeoutMs() { return writeTimeoutMs; }
        public void setWriteTimeoutMs(int writeTimeoutMs) { this.writeTimeoutMs = writeTimeoutMs; }
        public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }
    }
    
    /**
     * Audio processing configuration
     */
    public static class AudioConfig {
        
        // Audio validation
        
        private int minAudioSizeBytes = 1000;
        
        
        private int maxAudioSizeBytes = 25 * 1024 * 1024; // 25MB
        
        // TTS settings
        private String defaultVoiceModel = "alloy";
        private List<String> supportedVoiceModels = List.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");
        private String defaultTtsModel = "tts-1";
        
        // STT settings
        private String defaultSttModel = "whisper-1";
        private String defaultLanguage = "en";
        private List<String> supportedLanguages = List.of("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh");
        
        // Audio processing
        private boolean enableAudioValidation = true;
        private boolean enableAudioCaching = true;
        private int audioCacheMaxSize = 100;
        private int audioCacheTtlMinutes = 60;
        
        // Getters and setters
        public int getMinAudioSizeBytes() { return minAudioSizeBytes; }
        public void setMinAudioSizeBytes(int minAudioSizeBytes) { this.minAudioSizeBytes = minAudioSizeBytes; }
        public int getMaxAudioSizeBytes() { return maxAudioSizeBytes; }
        public void setMaxAudioSizeBytes(int maxAudioSizeBytes) { this.maxAudioSizeBytes = maxAudioSizeBytes; }
        public String getDefaultVoiceModel() { return defaultVoiceModel; }
        public void setDefaultVoiceModel(String defaultVoiceModel) { this.defaultVoiceModel = defaultVoiceModel; }
        public List<String> getSupportedVoiceModels() { return supportedVoiceModels; }
        public void setSupportedVoiceModels(List<String> supportedVoiceModels) { this.supportedVoiceModels = supportedVoiceModels; }
        public String getDefaultTtsModel() { return defaultTtsModel; }
        public void setDefaultTtsModel(String defaultTtsModel) { this.defaultTtsModel = defaultTtsModel; }
        public String getDefaultSttModel() { return defaultSttModel; }
        public void setDefaultSttModel(String defaultSttModel) { this.defaultSttModel = defaultSttModel; }
        public String getDefaultLanguage() { return defaultLanguage; }
        public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
        public List<String> getSupportedLanguages() { return supportedLanguages; }
        public void setSupportedLanguages(List<String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }
        public boolean isEnableAudioValidation() { return enableAudioValidation; }
        public void setEnableAudioValidation(boolean enableAudioValidation) { this.enableAudioValidation = enableAudioValidation; }
        public boolean isEnableAudioCaching() { return enableAudioCaching; }
        public void setEnableAudioCaching(boolean enableAudioCaching) { this.enableAudioCaching = enableAudioCaching; }
        public int getAudioCacheMaxSize() { return audioCacheMaxSize; }
        public void setAudioCacheMaxSize(int audioCacheMaxSize) { this.audioCacheMaxSize = audioCacheMaxSize; }
        public int getAudioCacheTtlMinutes() { return audioCacheTtlMinutes; }
        public void setAudioCacheTtlMinutes(int audioCacheTtlMinutes) { this.audioCacheTtlMinutes = audioCacheTtlMinutes; }
    }
    
    /**
     * Session management configuration
     */
    public static class SessionConfig {
        
        
        
        private int maxSessionsPerClient = 3;
        
        
        private long sessionTimeoutMs = 30 * 60 * 1000; // 30 minutes
        
        
        private long inactivityTimeoutMs = 10 * 60 * 1000; // 10 minutes
        
        // Session cleanup
        private boolean enableAutoCleanup = true;
        private int cleanupIntervalMinutes = 5;
        
        // Session history
        
        
        private int maxHistoryItems = 100;
        
        // Getters and setters
        public int getMaxSessionsPerClient() { return maxSessionsPerClient; }
        public void setMaxSessionsPerClient(int maxSessionsPerClient) { this.maxSessionsPerClient = maxSessionsPerClient; }
        public long getSessionTimeoutMs() { return sessionTimeoutMs; }
        public void setSessionTimeoutMs(long sessionTimeoutMs) { this.sessionTimeoutMs = sessionTimeoutMs; }
        public long getInactivityTimeoutMs() { return inactivityTimeoutMs; }
        public void setInactivityTimeoutMs(long inactivityTimeoutMs) { this.inactivityTimeoutMs = inactivityTimeoutMs; }
        public boolean isEnableAutoCleanup() { return enableAutoCleanup; }
        public void setEnableAutoCleanup(boolean enableAutoCleanup) { this.enableAutoCleanup = enableAutoCleanup; }
        public int getCleanupIntervalMinutes() { return cleanupIntervalMinutes; }
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) { this.cleanupIntervalMinutes = cleanupIntervalMinutes; }
        public int getMaxHistoryItems() { return maxHistoryItems; }
        public void setMaxHistoryItems(int maxHistoryItems) { this.maxHistoryItems = maxHistoryItems; }
    }
    
    /**
     * Speaker identification configuration
     */
    public static class SpeakerConfig {
        
        private boolean enableSpeakerIdentification = true;
        
        
        
        private int maxSpeakersPerSession = 5;
        
        // Confidence thresholds
        
        
        private double identificationThreshold = 0.7;
        
        
        
        private double enrollmentThreshold = 0.8;
        
        // Speaker data management
        private boolean enableSpeakerEnrollment = true;
        private boolean persistSpeakerProfiles = true;
        private int speakerProfileRetentionDays = 30;
        
        // Getters and setters
        public boolean isEnableSpeakerIdentification() { return enableSpeakerIdentification; }
        public void setEnableSpeakerIdentification(boolean enableSpeakerIdentification) { this.enableSpeakerIdentification = enableSpeakerIdentification; }
        public int getMaxSpeakersPerSession() { return maxSpeakersPerSession; }
        public void setMaxSpeakersPerSession(int maxSpeakersPerSession) { this.maxSpeakersPerSession = maxSpeakersPerSession; }
        public double getIdentificationThreshold() { return identificationThreshold; }
        public void setIdentificationThreshold(double identificationThreshold) { this.identificationThreshold = identificationThreshold; }
        public double getEnrollmentThreshold() { return enrollmentThreshold; }
        public void setEnrollmentThreshold(double enrollmentThreshold) { this.enrollmentThreshold = enrollmentThreshold; }
        public boolean isEnableSpeakerEnrollment() { return enableSpeakerEnrollment; }
        public void setEnableSpeakerEnrollment(boolean enableSpeakerEnrollment) { this.enableSpeakerEnrollment = enableSpeakerEnrollment; }
        public boolean isPersistSpeakerProfiles() { return persistSpeakerProfiles; }
        public void setPersistSpeakerProfiles(boolean persistSpeakerProfiles) { this.persistSpeakerProfiles = persistSpeakerProfiles; }
        public int getSpeakerProfileRetentionDays() { return speakerProfileRetentionDays; }
        public void setSpeakerProfileRetentionDays(int speakerProfileRetentionDays) { this.speakerProfileRetentionDays = speakerProfileRetentionDays; }
    }
    
    /**
     * Conversation and turn management configuration
     */
    public static class ConversationConfig {
        
        private boolean enableTurnManagement = true;
        private String defaultConversationMode = "SINGLE_SPEAKER"; // SINGLE_SPEAKER, STRUCTURED, OPEN
        
        // Turn timing
        
        private int minTurnGapMs = 1000;
        
        
        private int turnTimeoutMs = 10000;
        
        
        private int responseTimeoutMs = 30000;
        
        // Queue management
        
        
        private int maxQueuedTurns = 5;
        
        private boolean enableTurnQueue = true;
        private boolean enableInterruptions = false;
        
        // Getters and setters
        public boolean isEnableTurnManagement() { return enableTurnManagement; }
        public void setEnableTurnManagement(boolean enableTurnManagement) { this.enableTurnManagement = enableTurnManagement; }
        public String getDefaultConversationMode() { return defaultConversationMode; }
        public void setDefaultConversationMode(String defaultConversationMode) { this.defaultConversationMode = defaultConversationMode; }
        public int getMinTurnGapMs() { return minTurnGapMs; }
        public void setMinTurnGapMs(int minTurnGapMs) { this.minTurnGapMs = minTurnGapMs; }
        public int getTurnTimeoutMs() { return turnTimeoutMs; }
        public void setTurnTimeoutMs(int turnTimeoutMs) { this.turnTimeoutMs = turnTimeoutMs; }
        public int getResponseTimeoutMs() { return responseTimeoutMs; }
        public void setResponseTimeoutMs(int responseTimeoutMs) { this.responseTimeoutMs = responseTimeoutMs; }
        public int getMaxQueuedTurns() { return maxQueuedTurns; }
        public void setMaxQueuedTurns(int maxQueuedTurns) { this.maxQueuedTurns = maxQueuedTurns; }
        public boolean isEnableTurnQueue() { return enableTurnQueue; }
        public void setEnableTurnQueue(boolean enableTurnQueue) { this.enableTurnQueue = enableTurnQueue; }
        public boolean isEnableInterruptions() { return enableInterruptions; }
        public void setEnableInterruptions(boolean enableInterruptions) { this.enableInterruptions = enableInterruptions; }
    }
    
    /**
     * Error handling configuration
     */
    public static class ErrorConfig {
        
        
        
        private int maxConsecutiveErrors = 3;
        
        
        private long errorTrackingDurationMs = 300000; // 5 minutes
        
        private boolean enableDetailedErrorLogging = true;
        private boolean enableErrorRecovery = true;
        private boolean enableUserFriendlyMessages = true;
        
        // Error response customization
        private Map<String, String> customErrorMessages = Map.of();
        
        // Getters and setters
        public int getMaxConsecutiveErrors() { return maxConsecutiveErrors; }
        public void setMaxConsecutiveErrors(int maxConsecutiveErrors) { this.maxConsecutiveErrors = maxConsecutiveErrors; }
        public long getErrorTrackingDurationMs() { return errorTrackingDurationMs; }
        public void setErrorTrackingDurationMs(long errorTrackingDurationMs) { this.errorTrackingDurationMs = errorTrackingDurationMs; }
        public boolean isEnableDetailedErrorLogging() { return enableDetailedErrorLogging; }
        public void setEnableDetailedErrorLogging(boolean enableDetailedErrorLogging) { this.enableDetailedErrorLogging = enableDetailedErrorLogging; }
        public boolean isEnableErrorRecovery() { return enableErrorRecovery; }
        public void setEnableErrorRecovery(boolean enableErrorRecovery) { this.enableErrorRecovery = enableErrorRecovery; }
        public boolean isEnableUserFriendlyMessages() { return enableUserFriendlyMessages; }
        public void setEnableUserFriendlyMessages(boolean enableUserFriendlyMessages) { this.enableUserFriendlyMessages = enableUserFriendlyMessages; }
        public Map<String, String> getCustomErrorMessages() { return customErrorMessages; }
        public void setCustomErrorMessages(Map<String, String> customErrorMessages) { this.customErrorMessages = customErrorMessages; }
    }
    
    /**
     * Performance and optimization configuration
     */
    public static class PerformanceConfig {
        
        // Thread pool settings
        
        private int corePoolSize = 5;
        
        
        private int maxPoolSize = 20;
        
        
        private int queueCapacity = 100;
        
        // Caching settings
        private boolean enableResponseCaching = true;
        private int responseCacheMaxSize = 1000;
        private int responseCacheTtlMinutes = 15;
        
        // Resource management
        private boolean enableResourceMonitoring = true;
        private int memoryThresholdPercent = 80;
        private int cpuThresholdPercent = 90;
        
        // Getters and setters
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public boolean isEnableResponseCaching() { return enableResponseCaching; }
        public void setEnableResponseCaching(boolean enableResponseCaching) { this.enableResponseCaching = enableResponseCaching; }
        public int getResponseCacheMaxSize() { return responseCacheMaxSize; }
        public void setResponseCacheMaxSize(int responseCacheMaxSize) { this.responseCacheMaxSize = responseCacheMaxSize; }
        public int getResponseCacheTtlMinutes() { return responseCacheTtlMinutes; }
        public void setResponseCacheTtlMinutes(int responseCacheTtlMinutes) { this.responseCacheTtlMinutes = responseCacheTtlMinutes; }
        public boolean isEnableResourceMonitoring() { return enableResourceMonitoring; }
        public void setEnableResourceMonitoring(boolean enableResourceMonitoring) { this.enableResourceMonitoring = enableResourceMonitoring; }
        public int getMemoryThresholdPercent() { return memoryThresholdPercent; }
        public void setMemoryThresholdPercent(int memoryThresholdPercent) { this.memoryThresholdPercent = memoryThresholdPercent; }
        public int getCpuThresholdPercent() { return cpuThresholdPercent; }
        public void setCpuThresholdPercent(int cpuThresholdPercent) { this.cpuThresholdPercent = cpuThresholdPercent; }
    }
    
    /**
     * Security configuration
     */
    public static class SecurityConfig {
        
        private boolean enableRateLimiting = true;
        private int rateLimitRequestsPerMinute = 100;
        private boolean enableInputValidation = true;
        private boolean enableOutputSanitization = true;
        
        // CORS settings
        private List<String> allowedOrigins = List.of("http://localhost:3000", "https://*.vercel.app");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        
        // Data privacy
        private boolean enableDataEncryption = false;
        private boolean logSensitiveData = false;
        private int dataRetentionDays = 30;
        
        // Getters and setters
        public boolean isEnableRateLimiting() { return enableRateLimiting; }
        public void setEnableRateLimiting(boolean enableRateLimiting) { this.enableRateLimiting = enableRateLimiting; }
        public int getRateLimitRequestsPerMinute() { return rateLimitRequestsPerMinute; }
        public void setRateLimitRequestsPerMinute(int rateLimitRequestsPerMinute) { this.rateLimitRequestsPerMinute = rateLimitRequestsPerMinute; }
        public boolean isEnableInputValidation() { return enableInputValidation; }
        public void setEnableInputValidation(boolean enableInputValidation) { this.enableInputValidation = enableInputValidation; }
        public boolean isEnableOutputSanitization() { return enableOutputSanitization; }
        public void setEnableOutputSanitization(boolean enableOutputSanitization) { this.enableOutputSanitization = enableOutputSanitization; }
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        public boolean isEnableDataEncryption() { return enableDataEncryption; }
        public void setEnableDataEncryption(boolean enableDataEncryption) { this.enableDataEncryption = enableDataEncryption; }
        public boolean isLogSensitiveData() { return logSensitiveData; }
        public void setLogSensitiveData(boolean logSensitiveData) { this.logSensitiveData = logSensitiveData; }
        public int getDataRetentionDays() { return dataRetentionDays; }
        public void setDataRetentionDays(int dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    }
}