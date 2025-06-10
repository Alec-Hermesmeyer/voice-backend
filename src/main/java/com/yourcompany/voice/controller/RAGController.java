package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.RAGService;
import com.yourcompany.voice.service.RAGService.KnowledgeDocument;
import com.yourcompany.voice.service.RAGService.RAGResponse;
import com.yourcompany.voice.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST Controller for RAG (Retrieval-Augmented Generation) operations
 * Manages client knowledge bases and processes voice commands with context
 */
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = {"http://localhost:3000", "https://*.vercel.app"})
public class RAGController {

    @Autowired
    private RAGService ragService;
    
    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * Initialize or update a client's knowledge base
     */
    @PostMapping("/clients/{clientId}/knowledge-base")
    public ResponseEntity<InitializeKnowledgeBaseResponse> initializeKnowledgeBase(
            @PathVariable String clientId,
            @RequestBody InitializeKnowledgeBaseRequest request) {
        
        try {
            List<KnowledgeDocument> documents = new ArrayList<>();
            for (DocumentRequest docRequest : request.getDocuments()) {
                KnowledgeDocument doc = new KnowledgeDocument(
                    docRequest.getId(),
                    docRequest.getContent(),
                    docRequest.getSource(),
                    docRequest.getMetadata()
                );
                documents.add(doc);
            }
            
            ragService.initializeClientKnowledgeBase(clientId, documents);
            
            return ResponseEntity.ok(new InitializeKnowledgeBaseResponse(
                true,
                "Knowledge base initialized successfully for client: " + clientId,
                documents.size(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new InitializeKnowledgeBaseResponse(
                false,
                "Failed to initialize knowledge base: " + e.getMessage(),
                0,
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Add a single document to a client's knowledge base
     */
    @PostMapping("/clients/{clientId}/documents")
    public ResponseEntity<AddDocumentResponse> addDocument(
            @PathVariable String clientId,
            @RequestBody DocumentRequest documentRequest) {
        
        try {
            KnowledgeDocument document = new KnowledgeDocument(
                documentRequest.getId(),
                documentRequest.getContent(),
                documentRequest.getSource(),
                documentRequest.getMetadata()
            );
            
            ragService.addDocumentToClient(clientId, document);
            
            return ResponseEntity.ok(new AddDocumentResponse(
                true,
                "Document added successfully to client: " + clientId,
                documentRequest.getId(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new AddDocumentResponse(
                false,
                "Failed to add document: " + e.getMessage(),
                documentRequest.getId(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Upload and add documents from files
     */
    @PostMapping("/clients/{clientId}/upload")
    public ResponseEntity<UploadDocumentsResponse> uploadDocuments(
            @PathVariable String clientId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "source", defaultValue = "upload") String source) {
        
        try {
            List<String> processedFiles = new ArrayList<>();
            
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("filename", file.getOriginalFilename());
                    metadata.put("size", file.getSize());
                    metadata.put("contentType", file.getContentType());
                    metadata.put("uploadedAt", new Date());
                    
                    KnowledgeDocument document = new KnowledgeDocument(
                        clientId + "_" + file.getOriginalFilename() + "_" + System.currentTimeMillis(),
                        content,
                        source,
                        metadata
                    );
                    
                    ragService.addDocumentToClient(clientId, document);
                    processedFiles.add(file.getOriginalFilename());
                }
            }
            
            return ResponseEntity.ok(new UploadDocumentsResponse(
                true,
                "Documents uploaded successfully",
                processedFiles,
                processedFiles.size(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new UploadDocumentsResponse(
                false,
                "Failed to upload documents: " + e.getMessage(),
                new ArrayList<>(),
                0,
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Process voice command with RAG context
     */
    @PostMapping("/clients/{clientId}/voice-command")
    public ResponseEntity<RAGVoiceCommandResponse> processVoiceCommandWithRAG(
            @PathVariable String clientId,
            @RequestBody RAGVoiceCommandRequest request) {
        
        try {
            RAGResponse ragResponse = ragService.processVoiceCommandWithRAG(
                clientId, 
                request.getVoiceCommand(), 
                request.getCurrentContext()
            );
            
            return ResponseEntity.ok(new RAGVoiceCommandResponse(
                ragResponse.isSuccess(),
                ragResponse.getResponse(),
                ragResponse.getRelevantDocuments().size(),
                request.getVoiceCommand(),
                request.getCurrentContext(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new RAGVoiceCommandResponse(
                false,
                "Failed to process voice command: " + e.getMessage(),
                0,
                request.getVoiceCommand(),
                request.getCurrentContext(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Get client knowledge base statistics
     */
    @GetMapping("/clients/{clientId}/stats")
    public ResponseEntity<ClientStatsResponse> getClientStats(@PathVariable String clientId) {
        try {
            Map<String, Object> stats = ragService.getClientStats(clientId);
            
            return ResponseEntity.ok(new ClientStatsResponse(
                true,
                "Statistics retrieved successfully",
                clientId,
                stats,
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new ClientStatsResponse(
                false,
                "Failed to retrieve statistics: " + e.getMessage(),
                clientId,
                new HashMap<>(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Get all clients' statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AllClientsStatsResponse> getAllClientsStats() {
        try {
            Map<String, VectorStoreService.VectorStoreStats> allStats = vectorStoreService.getAllClientStats();
            
            return ResponseEntity.ok(new AllClientsStatsResponse(
                true,
                "All client statistics retrieved successfully",
                allStats,
                allStats.size(),
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new AllClientsStatsResponse(
                false,
                "Failed to retrieve all client statistics: " + e.getMessage(),
                new HashMap<>(),
                0,
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * Delete a client's knowledge base
     */
    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<DeleteClientResponse> deleteClient(@PathVariable String clientId) {
        try {
            vectorStoreService.deleteClientEmbeddings(clientId);
            
            return ResponseEntity.ok(new DeleteClientResponse(
                true,
                "Client knowledge base deleted successfully: " + clientId,
                clientId,
                System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new DeleteClientResponse(
                false,
                "Failed to delete client knowledge base: " + e.getMessage(),
                clientId,
                System.currentTimeMillis()
            ));
        }
    }

    // Request/Response DTOs

    public static class InitializeKnowledgeBaseRequest {
        private List<DocumentRequest> documents;

        public List<DocumentRequest> getDocuments() { return documents; }
        public void setDocuments(List<DocumentRequest> documents) { this.documents = documents; }
    }

    public static class DocumentRequest {
        private String id;
        private String content;
        private String source;
        private Map<String, Object> metadata;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class RAGVoiceCommandRequest {
        private String voiceCommand;
        private String currentContext;

        public String getVoiceCommand() { return voiceCommand; }
        public void setVoiceCommand(String voiceCommand) { this.voiceCommand = voiceCommand; }
        public String getCurrentContext() { return currentContext; }
        public void setCurrentContext(String currentContext) { this.currentContext = currentContext; }
    }

    public static class InitializeKnowledgeBaseResponse {
        private final boolean success;
        private final String message;
        private final int documentsProcessed;
        private final long timestamp;

        public InitializeKnowledgeBaseResponse(boolean success, String message, int documentsProcessed, long timestamp) {
            this.success = success;
            this.message = message;
            this.documentsProcessed = documentsProcessed;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getDocumentsProcessed() { return documentsProcessed; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AddDocumentResponse {
        private final boolean success;
        private final String message;
        private final String documentId;
        private final long timestamp;

        public AddDocumentResponse(boolean success, String message, String documentId, long timestamp) {
            this.success = success;
            this.message = message;
            this.documentId = documentId;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDocumentId() { return documentId; }
        public long getTimestamp() { return timestamp; }
    }

    public static class UploadDocumentsResponse {
        private final boolean success;
        private final String message;
        private final List<String> processedFiles;
        private final int filesProcessed;
        private final long timestamp;

        public UploadDocumentsResponse(boolean success, String message, List<String> processedFiles, int filesProcessed, long timestamp) {
            this.success = success;
            this.message = message;
            this.processedFiles = processedFiles;
            this.filesProcessed = filesProcessed;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<String> getProcessedFiles() { return processedFiles; }
        public int getFilesProcessed() { return filesProcessed; }
        public long getTimestamp() { return timestamp; }
    }

    public static class RAGVoiceCommandResponse {
        private final boolean success;
        private final String response;
        private final int relevantDocumentsCount;
        private final String originalCommand;
        private final String context;
        private final long timestamp;

        public RAGVoiceCommandResponse(boolean success, String response, int relevantDocumentsCount, 
                                     String originalCommand, String context, long timestamp) {
            this.success = success;
            this.response = response;
            this.relevantDocumentsCount = relevantDocumentsCount;
            this.originalCommand = originalCommand;
            this.context = context;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getResponse() { return response; }
        public int getRelevantDocumentsCount() { return relevantDocumentsCount; }
        public String getOriginalCommand() { return originalCommand; }
        public String getContext() { return context; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ClientStatsResponse {
        private final boolean success;
        private final String message;
        private final String clientId;
        private final Map<String, Object> stats;
        private final long timestamp;

        public ClientStatsResponse(boolean success, String message, String clientId, Map<String, Object> stats, long timestamp) {
            this.success = success;
            this.message = message;
            this.clientId = clientId;
            this.stats = stats;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getClientId() { return clientId; }
        public Map<String, Object> getStats() { return stats; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AllClientsStatsResponse {
        private final boolean success;
        private final String message;
        private final Map<String, VectorStoreService.VectorStoreStats> clientStats;
        private final int totalClients;
        private final long timestamp;

        public AllClientsStatsResponse(boolean success, String message, Map<String, VectorStoreService.VectorStoreStats> clientStats, 
                                     int totalClients, long timestamp) {
            this.success = success;
            this.message = message;
            this.clientStats = clientStats;
            this.totalClients = totalClients;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, VectorStoreService.VectorStoreStats> getClientStats() { return clientStats; }
        public int getTotalClients() { return totalClients; }
        public long getTimestamp() { return timestamp; }
    }

    public static class DeleteClientResponse {
        private final boolean success;
        private final String message;
        private final String clientId;
        private final long timestamp;

        public DeleteClientResponse(boolean success, String message, String clientId, long timestamp) {
            this.success = success;
            this.message = message;
            this.clientId = clientId;
            this.timestamp = timestamp;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getClientId() { return clientId; }
        public long getTimestamp() { return timestamp; }
    }
} 