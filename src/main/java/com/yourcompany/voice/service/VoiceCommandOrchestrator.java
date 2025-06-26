package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Orchestrates voice command processing across multiple services
 * Coordinates session management, speaker identification, turn management, and command processing
 */
@Service
public class VoiceCommandOrchestrator {
    
    private static final Logger logger = Logger.getLogger(VoiceCommandOrchestrator.class.getName());
    
    @Autowired
    private VoiceSessionService sessionService;
    
    @Autowired
    private AudioProcessingService audioProcessingService;
    
    @Autowired
    private CommandService commandService;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private VoiceResponseService voiceResponseService;
    
    @Autowired
    private UIControlService uiControlService;
    
    /**
     * Process complete voice interaction from audio input to response
     */
    public CompletableFuture<VoiceCommandResult> processVoiceCommand(
            String sessionId, 
            byte[] audioData, 
            String currentContext) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Get the session
                VoiceSession session = sessionService.getSession(sessionId);
                if (session == null) {
                    return VoiceCommandResult.error("Session not found: " + sessionId);
                }
                
                // Step 2: Validate and process audio
                AudioProcessingService.AudioValidationResult validation = 
                    audioProcessingService.validateAudio(audioData);
                
                if (!validation.isValid()) {
                    return VoiceCommandResult.error("Invalid audio: " + validation.getMessage());
                }
                
                // Step 3: Transcribe audio to text
                AudioProcessingService.STTResult sttResult = 
                    audioProcessingService.transcribeAudio(audioData, "en").join();
                
                if (!sttResult.isSuccess()) {
                    return VoiceCommandResult.error("Transcription failed: " + sttResult.getErrorMessage());
                }
                
                String transcript = sttResult.getTranscript();
                logger.info("Transcribed: " + transcript);
                
                // Step 4: Process voice input with speaker identification and turn management
                VoiceInput voiceInput = new VoiceInput(audioData, transcript, currentContext);
                VoiceSessionService.VoiceInputResult inputResult = 
                    sessionService.processVoiceInput(sessionId, voiceInput);
                
                if (!inputResult.isSuccess()) {
                    return VoiceCommandResult.error("Voice input processing failed: " + inputResult.getErrorMessage());
                }
                
                // Step 5: Handle turn management responses
                if (inputResult.isTurnManagement()) {
                    return handleTurnManagementResponse(sessionId, inputResult);
                }
                
                // Step 6: Process the actual command
                VoiceCommandProcessingResult processingResult = processCommand(
                    session, 
                    transcript, 
                    currentContext, 
                    inputResult.getSpeakerResult(),
                    inputResult.getTurnResult()
                );
                
                if (!processingResult.isSuccess()) {
                    return VoiceCommandResult.error("Command processing failed: " + processingResult.getErrorMessage());
                }
                
                // Step 7: Generate and send response
                return generateAndSendResponse(sessionId, processingResult);
                
            } catch (Exception e) {
                logger.severe("Error in voice command orchestration: " + e.getMessage());
                return VoiceCommandResult.error("Voice command processing failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handle turn management responses (when it's not someone's turn)
     */
    private VoiceCommandResult handleTurnManagementResponse(String sessionId, 
                                                           VoiceSessionService.VoiceInputResult inputResult) {
        try {
            String message = inputResult.getTurnManagementMessage();
            
            // Generate TTS for turn management message
            VoiceSession session = sessionService.getSession(sessionId);
            if (session != null && session.getConfig().isTtsEnabled()) {
                CompletableFuture<AudioProcessingService.TTSResult> ttsResult = 
                    audioProcessingService.generateTTS(message, session.getConfig().getVoiceModel());
                
                // Send audio response
                ttsResult.thenAccept(result -> {
                    if (result.isSuccess()) {
                        voiceResponseService.sendTTSResponse(sessionId, message, result.getAudioData());
                    } else {
                        voiceResponseService.sendTextResponse(sessionId, message);
                    }
                });
            } else {
                voiceResponseService.sendTextResponse(sessionId, message);
            }
            
            return VoiceCommandResult.turnManagement(message, inputResult.getTurnResult());
            
        } catch (Exception e) {
            logger.severe("Error handling turn management response: " + e.getMessage());
            return VoiceCommandResult.error("Turn management failed: " + e.getMessage());
        }
    }
    
    /**
     * Process the actual voice command
     */
    private VoiceCommandProcessingResult processCommand(VoiceSession session,
                                                       String transcript,
                                                       String currentContext,
                                                       SpeakerIdentificationService.SpeakerResult speakerResult,
                                                       ConversationTurnManager.TurnResult turnResult) {
        try {
            String clientId = session.getClientId();
            String speakerId = speakerResult.getSpeakerId();
            
            logger.info("Processing command from speaker: " + speakerId + " - " + transcript);
            
            // Try RAG processing first if client has knowledge base
            if (clientId != null) {
                RAGService.RAGResponse ragResponse = ragService.processVoiceCommandWithRAG(
                    clientId, transcript, currentContext);
                
                if (ragResponse.isSuccess() && ragResponse.getRelevantDocuments().size() > 0) {
                    // RAG found relevant information
                    String enhancedResponse = enhanceResponseWithContext(
                        ragResponse.getResponse(), speakerId, currentContext, session);
                    
                    // Check if response contains UI actions
                    if (containsUIActions(ragResponse.getResponse())) {
                        executeUIActions(ragResponse.getResponse(), currentContext, session);
                    }
                    
                    return VoiceCommandProcessingResult.success(
                        enhancedResponse, 
                        VoiceResponseType.RAG_RESPONSE,
                        ragResponse.getRelevantDocuments().size()
                    );
                }
            }
            
            // Fallback to standard command processing
            String commandResult = commandService.processVoiceCommand(transcript, currentContext, clientId);
            
            // Handle command errors
            if (commandResult.contains("COMMAND_NOT_RECOGNIZED") || commandResult.contains("ERROR")) {
                String errorResponse = generateErrorResponse(transcript, speakerId, currentContext);
                return VoiceCommandProcessingResult.success(
                    errorResponse, 
                    VoiceResponseType.ERROR,
                    0
                );
            }
            
            // Generate response for successful command
            String responseText = generateCommandResponse(commandResult, transcript, speakerId);
            String enhancedResponse = enhanceResponseWithContext(responseText, speakerId, currentContext, session);
            
            // Execute UI actions if needed
            if (commandResult.contains("NAVIGATION") || commandResult.contains("LLM_COMMAND_EXECUTED")) {
                // UI actions are handled by CommandService, we just need to acknowledge
            }
            
            return VoiceCommandProcessingResult.success(
                enhancedResponse, 
                VoiceResponseType.COMMAND_RESPONSE,
                0
            );
            
        } catch (Exception e) {
            logger.severe("Error processing command: " + e.getMessage());
            return VoiceCommandProcessingResult.error("Command processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Generate and send response to user
     */
    private VoiceCommandResult generateAndSendResponse(String sessionId, 
                                                      VoiceCommandProcessingResult processingResult) {
        try {
            VoiceSession session = sessionService.getSession(sessionId);
            String responseText = processingResult.getResponseText();
            
            // Update session with response
            session.setLastResponse(responseText);
            
            // Generate and send TTS if enabled
            if (session.getConfig().isTtsEnabled()) {
                CompletableFuture<AudioProcessingService.TTSResult> ttsResult = 
                    audioProcessingService.generateTTS(responseText, session.getConfig().getVoiceModel());
                
                ttsResult.thenAccept(result -> {
                    if (result.isSuccess()) {
                        voiceResponseService.sendTTSResponse(sessionId, responseText, result.getAudioData());
                    } else {
                        voiceResponseService.sendTextResponse(sessionId, responseText);
                    }
                });
            } else {
                voiceResponseService.sendTextResponse(sessionId, responseText);
            }
            
            return VoiceCommandResult.success(
                responseText, 
                processingResult.getResponseType(),
                processingResult.getContextualInfo()
            );
            
        } catch (Exception e) {
            logger.severe("Error generating response: " + e.getMessage());
            return VoiceCommandResult.error("Response generation failed: " + e.getMessage());
        }
    }
    
    /**
     * Enhance response with speaker and context information
     */
    private String enhanceResponseWithContext(String response, String speakerId, 
                                            String context, VoiceSession session) {
        // Add speaker-specific personalization
        if (session.getConfig().getExpectedSpeakers() > 1) {
            // In multi-speaker mode, we might want to address the speaker
            // But keep it natural - only add names if configured
        }
        
        // Add context-aware enhancements
        if (context != null && context.contains("error")) {
            response = "I notice you're having some issues. " + response;
        }
        
        return response;
    }
    
    /**
     * Check if response contains UI actions
     */
    private boolean containsUIActions(String response) {
        String lower = response.toLowerCase();
        return lower.contains("navigate") || lower.contains("click") || 
               lower.contains("go to") || lower.contains("open") ||
               lower.contains("type") || lower.contains("fill");
    }
    
    /**
     * Execute UI actions from response
     */
    private void executeUIActions(String response, String currentContext, VoiceSession session) {
        try {
            // This would extract and execute UI commands
            // For now, delegate to UIControlService
            uiControlService.processUICommand(response, currentContext, session.getClientId());
        } catch (Exception e) {
            logger.severe("Error executing UI actions: " + e.getMessage());
        }
    }
    
    /**
     * Generate error response with speaker context
     */
    private String generateErrorResponse(String transcript, String speakerId, String context) {
        return "I'm not sure how to help with that. Could you try rephrasing your request?";
    }
    
    /**
     * Generate response for successful command
     */
    private String generateCommandResponse(String commandResult, String transcript, String speakerId) {
        switch (commandResult) {
            case "LLM_COMMAND_EXECUTED":
                return "Done! I've completed that action for you.";
            case "SUBMIT":
            case "SEND":
                return "Perfect! I've submitted that for you.";
            case "CLEAR_CHAT":
                return "All clear! Chat history has been reset.";
            default:
                return "Task completed successfully.";
        }
    }
    
    // Result classes
    
    public static class VoiceCommandResult {
        private final boolean success;
        private final String responseText;
        private final VoiceResponseType responseType;
        private final String errorMessage;
        private final boolean isTurnManagement;
        private final ConversationTurnManager.TurnResult turnResult;
        private final Object contextualInfo;
        
        private VoiceCommandResult(boolean success, String responseText, VoiceResponseType responseType,
                                  String errorMessage, boolean isTurnManagement, 
                                  ConversationTurnManager.TurnResult turnResult, Object contextualInfo) {
            this.success = success;
            this.responseText = responseText;
            this.responseType = responseType;
            this.errorMessage = errorMessage;
            this.isTurnManagement = isTurnManagement;
            this.turnResult = turnResult;
            this.contextualInfo = contextualInfo;
        }
        
        public static VoiceCommandResult success(String responseText, VoiceResponseType responseType, Object contextualInfo) {
            return new VoiceCommandResult(true, responseText, responseType, null, false, null, contextualInfo);
        }
        
        public static VoiceCommandResult error(String errorMessage) {
            return new VoiceCommandResult(false, null, VoiceResponseType.ERROR, errorMessage, false, null, null);
        }
        
        public static VoiceCommandResult turnManagement(String message, ConversationTurnManager.TurnResult turnResult) {
            return new VoiceCommandResult(true, message, VoiceResponseType.TURN_MANAGEMENT, null, true, turnResult, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getResponseText() { return responseText; }
        public VoiceResponseType getResponseType() { return responseType; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isTurnManagement() { return isTurnManagement; }
        public ConversationTurnManager.TurnResult getTurnResult() { return turnResult; }
        public Object getContextualInfo() { return contextualInfo; }
    }
    
    private static class VoiceCommandProcessingResult {
        private final boolean success;
        private final String responseText;
        private final VoiceResponseType responseType;
        private final String errorMessage;
        private final Object contextualInfo;
        
        private VoiceCommandProcessingResult(boolean success, String responseText, 
                                           VoiceResponseType responseType, String errorMessage, 
                                           Object contextualInfo) {
            this.success = success;
            this.responseText = responseText;
            this.responseType = responseType;
            this.errorMessage = errorMessage;
            this.contextualInfo = contextualInfo;
        }
        
        public static VoiceCommandProcessingResult success(String responseText, VoiceResponseType responseType, Object contextualInfo) {
            return new VoiceCommandProcessingResult(true, responseText, responseType, null, contextualInfo);
        }
        
        public static VoiceCommandProcessingResult error(String errorMessage) {
            return new VoiceCommandProcessingResult(false, null, VoiceResponseType.ERROR, errorMessage, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getResponseText() { return responseText; }
        public VoiceResponseType getResponseType() { return responseType; }
        public String getErrorMessage() { return errorMessage; }
        public Object getContextualInfo() { return contextualInfo; }
    }
}

/**
 * Voice response types including new turn management type
 */
enum VoiceResponseType {
    RAG_RESPONSE,
    COMMAND_RESPONSE, 
    CONTROL,
    HELP,
    REPEAT,
    ERROR,
    TURN_MANAGEMENT
}