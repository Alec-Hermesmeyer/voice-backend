package com.yourcompany.voice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RAG (Retrieval-Augmented Generation) Service for multiple clients
 * Manages client-specific knowledge bases and provides context-aware responses
 */
@Service
public class RAGService {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private final OkHttpClient client = new OkHttpClient();
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    // Client-specific knowledge bases
    private final Map<String, ClientKnowledgeBase> clientKnowledgeBases = new ConcurrentHashMap<>();
    
    // Embedding cache to avoid redundant API calls
    private final Map<String, List<Double>> embeddingCache = new ConcurrentHashMap<>();
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    /**
     * Initialize or update a client's knowledge base
     */
    public void initializeClientKnowledgeBase(String clientId, List<KnowledgeDocument> documents) {
        try {
            ClientKnowledgeBase knowledgeBase = new ClientKnowledgeBase(clientId);
            
            for (KnowledgeDocument doc : documents) {
                // Generate embeddings for document chunks
                List<DocumentChunk> chunks = chunkDocument(doc);
                for (DocumentChunk chunk : chunks) {
                    List<Double> embedding = generateEmbedding(chunk.getContent());
                    chunk.setEmbedding(embedding);
                    knowledgeBase.addDocument(chunk);
                }
            }
            
            clientKnowledgeBases.put(clientId, knowledgeBase);
            System.out.println("✅ Initialized knowledge base for client: " + clientId + 
                             " with " + knowledgeBase.getDocumentCount() + " document chunks");
                             
        } catch (Exception e) {
            System.err.println("❌ Error initializing knowledge base for client " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Process voice command with RAG context for specific client
     */
    public RAGResponse processVoiceCommandWithRAG(String clientId, String voiceCommand, String currentContext) {
        try {
            ClientKnowledgeBase knowledgeBase = clientKnowledgeBases.get(clientId);
            if (knowledgeBase == null) {
                return new RAGResponse(false, "No knowledge base found for client: " + clientId, 
                                     new ArrayList<>(), voiceCommand);
            }
            
            // Retrieve relevant documents
            List<DocumentChunk> relevantChunks = retrieveRelevantDocuments(voiceCommand, knowledgeBase);
            
            // Generate response using retrieved context
            JSONObject response = generateAugmentedResponse(voiceCommand, relevantChunks, currentContext, clientId);
            
            return new RAGResponse(
                response.optBoolean("success", true),
                response.optString("response", ""),
                relevantChunks,
                voiceCommand
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error processing RAG command: " + e.getMessage());
            return new RAGResponse(false, "Error processing command: " + e.getMessage(), 
                                 new ArrayList<>(), voiceCommand);
        }
    }
    
    /**
     * Retrieve relevant documents based on voice command
     */
    private List<DocumentChunk> retrieveRelevantDocuments(String query, ClientKnowledgeBase knowledgeBase) {
        try {
            // Generate embedding for the query
            List<Double> queryEmbedding = generateEmbedding(query);
            
            // Calculate similarity scores and rank documents
            List<DocumentScore> scores = new ArrayList<>();
            for (DocumentChunk chunk : knowledgeBase.getDocuments()) {
                double similarity = calculateCosineSimilarity(queryEmbedding, chunk.getEmbedding());
                scores.add(new DocumentScore(chunk, similarity));
            }
            
            // Sort by similarity and return top results
            scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            
            // Return top 5 most relevant chunks
            int maxResults = Math.min(5, scores.size());
            List<DocumentChunk> relevantChunks = new ArrayList<>();
            for (int i = 0; i < maxResults; i++) {
                if (scores.get(i).getScore() > 0.7) { // Only include highly relevant chunks
                    relevantChunks.add(scores.get(i).getChunk());
                }
            }
            
            return relevantChunks;
            
        } catch (Exception e) {
            System.err.println("❌ Error retrieving relevant documents: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate augmented response using LLM with retrieved context
     */
    private JSONObject generateAugmentedResponse(String query, List<DocumentChunk> context, 
                                               String currentUIContext, String clientId) throws JSONException, IOException {
        
        // Build context string from retrieved documents
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Client ID: ").append(clientId).append("\n");
        contextBuilder.append("Current UI Context: ").append(currentUIContext).append("\n");
        contextBuilder.append("Relevant Knowledge Base Information:\n");
        
        for (int i = 0; i < context.size(); i++) {
            DocumentChunk chunk = context.get(i);
            contextBuilder.append(String.format("[Document %d - %s]: %s\n", 
                                i + 1, chunk.getSource(), chunk.getContent()));
        }
        
        // Construct LLM prompt
        JSONArray messages = new JSONArray()
            .put(new JSONObject()
                .put("role", "system")
                .put("content", buildSystemPrompt()))
            .put(new JSONObject()
                .put("role", "user")
                .put("content", String.format("Context:\n%s\n\nUser Query: %s", 
                                            contextBuilder.toString(), query)));
        
        // Call OpenAI API
        JSONObject requestBody = new JSONObject()
            .put("model", "gpt-4-turbo-preview")
            .put("messages", messages)
            .put("temperature", 0.3)
            .put("max_tokens", 500);
        
        Request request = new Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer " + openAiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")))
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API error: " + response.code());
            }
            
            JSONObject responseJson = new JSONObject(response.body().string());
            String content = responseJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
            
            return new JSONObject()
                .put("success", true)
                .put("response", content)
                .put("context_used", context.size());
        }
    }
    
    /**
     * Generate embeddings for text using OpenAI API
     */
    private List<Double> generateEmbedding(String text) throws IOException {
        // Check cache first
        String cacheKey = text.hashCode() + "";
        if (embeddingCache.containsKey(cacheKey)) {
            return embeddingCache.get(cacheKey);
        }
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("input", text)
                .put("model", "text-embedding-ada-002");
            
            Request request = new Request.Builder()
                .url(OPENAI_EMBEDDING_URL)
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("OpenAI Embedding API error: " + response.code());
                }
                
                JSONObject responseJson = new JSONObject(response.body().string());
                JSONArray embeddingArray = responseJson.getJSONArray("data")
                    .getJSONObject(0)
                    .getJSONArray("embedding");
                
                List<Double> embedding = new ArrayList<>();
                for (int i = 0; i < embeddingArray.length(); i++) {
                    embedding.add(embeddingArray.getDouble(i));
                }
                
                // Cache the embedding
                embeddingCache.put(cacheKey, embedding);
                return embedding;
            }
        } catch (JSONException e) {
            throw new IOException("Error creating JSON for embedding request", e);
        }
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Chunk a document into smaller pieces for better retrieval
     */
    private List<DocumentChunk> chunkDocument(KnowledgeDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = document.getContent();
        int chunkSize = 500; // Characters per chunk
        int overlap = 50; // Overlap between chunks
        
        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, content.length());
            String chunkContent = content.substring(i, end);
            
            DocumentChunk chunk = new DocumentChunk(
                document.getId() + "_chunk_" + (i / (chunkSize - overlap)),
                chunkContent,
                document.getSource(),
                document.getMetadata()
            );
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    /**
     * Build system prompt for RAG responses
     */
    private String buildSystemPrompt() {
        return "You are an intelligent voice assistant with access to client-specific knowledge bases. " +
               "Your task is to provide helpful, accurate responses based on the provided context. " +
               "When answering questions or processing commands:\n" +
               "1. Use the provided knowledge base information to give accurate, contextual responses\n" +
               "2. If the query relates to UI navigation or actions, provide clear guidance\n" +
               "3. If information is not available in the knowledge base, say so clearly\n" +
               "4. Maintain a conversational and helpful tone\n" +
               "5. Reference specific document sources when relevant\n" +
               "6. Keep responses concise but informative\n\n" +
               "Always prioritize accuracy and be transparent about the limitations of your knowledge.";
    }
    
    /**
     * Add a new document to a client's knowledge base
     */
    public void addDocumentToClient(String clientId, KnowledgeDocument document) {
        ClientKnowledgeBase knowledgeBase = clientKnowledgeBases.get(clientId);
        if (knowledgeBase == null) {
            knowledgeBase = new ClientKnowledgeBase(clientId);
            clientKnowledgeBases.put(clientId, knowledgeBase);
        }
        
        try {
            List<DocumentChunk> chunks = chunkDocument(document);
            for (DocumentChunk chunk : chunks) {
                List<Double> embedding = generateEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                knowledgeBase.addDocument(chunk);
            }
            
            System.out.println("✅ Added document to client " + clientId + " knowledge base: " + document.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Error adding document to client " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get client knowledge base statistics
     */
    public Map<String, Object> getClientStats(String clientId) {
        ClientKnowledgeBase knowledgeBase = clientKnowledgeBases.get(clientId);
        Map<String, Object> stats = new HashMap<>();
        
        if (knowledgeBase != null) {
            stats.put("clientId", clientId);
            stats.put("documentCount", knowledgeBase.getDocumentCount());
            stats.put("lastUpdated", knowledgeBase.getLastUpdated());
            stats.put("totalChunks", knowledgeBase.getDocuments().size());
        } else {
            stats.put("clientId", clientId);
            stats.put("status", "No knowledge base found");
        }
        
        return stats;
    }
    
    // Inner classes for data structures
    
    public static class ClientKnowledgeBase {
        private final String clientId;
        private final List<DocumentChunk> documents;
        private final Date lastUpdated;
        
        public ClientKnowledgeBase(String clientId) {
            this.clientId = clientId;
            this.documents = new ArrayList<>();
            this.lastUpdated = new Date();
        }
        
        public void addDocument(DocumentChunk chunk) {
            documents.add(chunk);
        }
        
        public List<DocumentChunk> getDocuments() {
            return documents;
        }
        
        public int getDocumentCount() {
            return documents.size();
        }
        
        public Date getLastUpdated() {
            return lastUpdated;
        }
    }
    
    public static class KnowledgeDocument {
        private final String id;
        private final String content;
        private final String source;
        private final Map<String, Object> metadata;
        
        public KnowledgeDocument(String id, String content, String source, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.source = source;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public String getId() { return id; }
        public String getContent() { return content; }
        public String getSource() { return source; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public static class DocumentChunk {
        private final String id;
        private final String content;
        private final String source;
        private final Map<String, Object> metadata;
        private List<Double> embedding;
        
        public DocumentChunk(String id, String content, String source, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.source = source;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getContent() { return content; }
        public String getSource() { return source; }
        public Map<String, Object> getMetadata() { return metadata; }
        public List<Double> getEmbedding() { return embedding; }
        public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    }
    
    public static class DocumentScore {
        private final DocumentChunk chunk;
        private final double score;
        
        public DocumentScore(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
        
        public DocumentChunk getChunk() { return chunk; }
        public double getScore() { return score; }
    }
    
    public static class RAGResponse {
        private final boolean success;
        private final String response;
        private final List<DocumentChunk> relevantDocuments;
        private final String originalQuery;
        
        public RAGResponse(boolean success, String response, List<DocumentChunk> relevantDocuments, String originalQuery) {
            this.success = success;
            this.response = response;
            this.relevantDocuments = relevantDocuments;
            this.originalQuery = originalQuery;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getResponse() { return response; }
        public List<DocumentChunk> getRelevantDocuments() { return relevantDocuments; }
        public String getOriginalQuery() { return originalQuery; }
    }
} 