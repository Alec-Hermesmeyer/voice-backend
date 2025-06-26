package com.yourcompany.voice.exception;

/**
 * Base exception for all voice-related errors
 * Provides consistent error handling across the voice system
 */
public class VoiceException extends Exception {
    
    private final VoiceErrorCode errorCode;
    private final String userFriendlyMessage;
    private final boolean recoverable;
    
    public VoiceException(VoiceErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getUserFriendlyMessage();
        this.recoverable = errorCode.isRecoverable();
    }
    
    public VoiceException(VoiceErrorCode errorCode, String message, String userFriendlyMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userFriendlyMessage = userFriendlyMessage;
        this.recoverable = errorCode.isRecoverable();
    }
    
    public VoiceException(VoiceErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getUserFriendlyMessage();
        this.recoverable = errorCode.isRecoverable();
    }
    
    public VoiceException(VoiceErrorCode errorCode, String message, String userFriendlyMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = userFriendlyMessage;
        this.recoverable = errorCode.isRecoverable();
    }
    
    // Getters
    public VoiceErrorCode getErrorCode() { return errorCode; }
    public String getUserFriendlyMessage() { return userFriendlyMessage; }
    public boolean isRecoverable() { return recoverable; }
    public String getErrorId() { return errorCode.getCode(); }
}

/**
 * Runtime exception for voice-related errors that shouldn't be checked
 */
class VoiceRuntimeException extends RuntimeException {
    
    private final VoiceErrorCode errorCode;
    private final String userFriendlyMessage;
    private final boolean recoverable;
    
    public VoiceRuntimeException(VoiceErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getUserFriendlyMessage();
        this.recoverable = errorCode.isRecoverable();
    }
    
    public VoiceRuntimeException(VoiceErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getUserFriendlyMessage();
        this.recoverable = errorCode.isRecoverable();
    }
    
    // Getters
    public VoiceErrorCode getErrorCode() { return errorCode; }
    public String getUserFriendlyMessage() { return userFriendlyMessage; }
    public boolean isRecoverable() { return recoverable; }
    public String getErrorId() { return errorCode.getCode(); }
}

/**
 * Standardized error codes for the voice system
 */
enum VoiceErrorCode {
    
    // Session Errors
    SESSION_NOT_FOUND("VOICE_001", "Session not found", "I can't find your voice session. Please start a new session.", true),
    SESSION_EXPIRED("VOICE_002", "Session expired", "Your voice session has expired. Please start a new session.", true),
    SESSION_LIMIT_EXCEEDED("VOICE_003", "Session limit exceeded", "Too many active sessions. Please try again later.", true),
    
    // Audio Processing Errors
    AUDIO_INVALID("VOICE_101", "Invalid audio data", "I couldn't process that audio. Please try speaking again.", true),
    AUDIO_TOO_SHORT("VOICE_102", "Audio too short", "I didn't catch that. Could you speak a bit longer?", true),
    AUDIO_TOO_LONG("VOICE_103", "Audio too long", "That was a bit long. Please keep your message shorter.", true),
    AUDIO_FORMAT_UNSUPPORTED("VOICE_104", "Unsupported audio format", "I can't process that audio format. Please try again.", true),
    
    // Speech Recognition Errors
    STT_FAILED("VOICE_201", "Speech recognition failed", "I couldn't understand what you said. Please try again.", true),
    STT_API_ERROR("VOICE_202", "Speech recognition API error", "I'm having trouble hearing you right now. Please try again.", true),
    STT_NO_SPEECH("VOICE_203", "No speech detected", "I didn't hear anything. Could you please speak up?", true),
    
    // Text-to-Speech Errors
    TTS_FAILED("VOICE_301", "Text-to-speech failed", "I can't speak right now, but I can still help you.", true),
    TTS_API_ERROR("VOICE_302", "Text-to-speech API error", "I'm having trouble speaking right now. Please read my text response.", true),
    
    // Speaker Identification Errors
    SPEAKER_ID_FAILED("VOICE_401", "Speaker identification failed", "I'm having trouble identifying who's speaking. Please continue anyway.", true),
    SPEAKER_NOT_ENROLLED("VOICE_402", "Speaker not enrolled", "I don't recognize your voice. Please enroll first or continue as a new speaker.", true),
    SPEAKER_CONFIDENCE_LOW("VOICE_403", "Low speaker confidence", "I'm not sure who's speaking, but I'll do my best to help.", true),
    
    // Turn Management Errors
    TURN_NOT_ALLOWED("VOICE_501", "Turn not allowed", "Please wait your turn to speak.", true),
    TURN_TIMEOUT("VOICE_502", "Turn timeout", "I didn't hear a response in time. Please try again.", true),
    CONVERSATION_ENDED("VOICE_503", "Conversation ended", "This conversation has ended. Please start a new session.", true),
    
    // Command Processing Errors
    COMMAND_NOT_RECOGNIZED("VOICE_601", "Command not recognized", "I didn't understand that command. Could you try rephrasing?", true),
    COMMAND_PROCESSING_FAILED("VOICE_602", "Command processing failed", "I couldn't complete that action. Please try again.", true),
    COMMAND_TIMEOUT("VOICE_603", "Command timeout", "That action took too long. Please try again.", true),
    
    // RAG System Errors
    RAG_SEARCH_FAILED("VOICE_701", "Knowledge search failed", "I couldn't search the knowledge base right now. Let me try to help anyway.", true),
    RAG_NO_RESULTS("VOICE_702", "No relevant information found", "I couldn't find specific information about that. Could you be more specific?", true),
    RAG_API_ERROR("VOICE_703", "Knowledge system error", "The knowledge system is having issues. I'll try to help with what I know.", true),
    
    // API Integration Errors
    OPENAI_API_ERROR("VOICE_801", "OpenAI API error", "I'm having trouble connecting to my AI services. Please try again.", true),
    DEEPGRAM_API_ERROR("VOICE_802", "Deepgram API error", "Speech recognition service is having issues. Please try again.", true),
    API_RATE_LIMITED("VOICE_803", "API rate limited", "I'm being asked to do too much right now. Please wait a moment and try again.", true),
    API_QUOTA_EXCEEDED("VOICE_804", "API quota exceeded", "I've reached my usage limit. Please try again later.", false),
    
    // Configuration Errors
    CONFIG_MISSING("VOICE_901", "Configuration missing", "System configuration is incomplete. Please contact support.", false),
    API_KEY_INVALID("VOICE_902", "Invalid API key", "System authentication failed. Please contact support.", false),
    
    // System Errors
    INTERNAL_ERROR("VOICE_999", "Internal system error", "Something went wrong on my end. Please try again.", true),
    SERVICE_UNAVAILABLE("VOICE_998", "Service unavailable", "The voice service is temporarily unavailable. Please try again later.", true);
    
    private final String code;
    private final String technicalMessage;
    private final String userFriendlyMessage;
    private final boolean recoverable;
    
    VoiceErrorCode(String code, String technicalMessage, String userFriendlyMessage, boolean recoverable) {
        this.code = code;
        this.technicalMessage = technicalMessage;
        this.userFriendlyMessage = userFriendlyMessage;
        this.recoverable = recoverable;
    }
    
    // Getters
    public String getCode() { return code; }
    public String getTechnicalMessage() { return technicalMessage; }
    public String getUserFriendlyMessage() { return userFriendlyMessage; }
    public boolean isRecoverable() { return recoverable; }
}