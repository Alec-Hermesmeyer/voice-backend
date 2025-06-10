package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Vector Store Service for persisting and managing document embeddings
 * Provides local file-based storage for vector embeddings and metadata
 */
@Service
public class VectorStoreService {
    
    private static final String VECTOR_STORE_DIR = "./vector-store";
    private static final String EMBEDDINGS_SUFFIX = "_embeddings.json";
    private static final String METADATA_SUFFIX = "_metadata.json";
    
    // In-memory cache for fast access
    private final Map<String, Map<String, List<Double>>> clientEmbeddingsCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Object>>> clientMetadataCache = new ConcurrentHashMap<>();
    
    public VectorStoreService() {
        initializeVectorStore();
    }
    
    /**
     * Initialize vector store directory structure
     */
    private void initializeVectorStore() {
        try {
            Path vectorStoreePath = Paths.get(VECTOR_STORE_DIR);
            if (!Files.exists(vectorStoreePath)) {
                Files.createDirectories(vectorStoreePath);
                System.out.println("✅ Created vector store directory: " + VECTOR_STORE_DIR);
            }
        } catch (IOException e) {
            System.err.println("❌ Error creating vector store directory: " + e.getMessage());
        }
    }
    
    /**
     * Store embeddings for a client's document chunk
     */
    public void storeEmbedding(String clientId, String documentId, List<Double> embedding, Map<String, Object> metadata) {
        try {
            // Update in-memory cache
            clientEmbeddingsCache.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>()).put(documentId, embedding);
            clientMetadataCache.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>()).put(documentId, metadata);
            
            // Persist to file system
            persistClientEmbeddings(clientId);
            persistClientMetadata(clientId);
            
        } catch (Exception e) {
            System.err.println("❌ Error storing embedding for client " + clientId + ", document " + documentId + ": " + e.getMessage());
        }
    }
    
    /**
     * Retrieve embeddings for a specific client
     */
    public Map<String, List<Double>> getClientEmbeddings(String clientId) {
        Map<String, List<Double>> embeddings = clientEmbeddingsCache.get(clientId);
        if (embeddings == null) {
            // Try to load from file system
            embeddings = loadClientEmbeddings(clientId);
            if (embeddings != null) {
                clientEmbeddingsCache.put(clientId, embeddings);
            }
        }
        return embeddings != null ? embeddings : new HashMap<>();
    }
    
    /**
     * Retrieve metadata for a specific client
     */
    public Map<String, Map<String, Object>> getClientMetadata(String clientId) {
        Map<String, Map<String, Object>> metadata = clientMetadataCache.get(clientId);
        if (metadata == null) {
            // Try to load from file system
            metadata = loadClientMetadata(clientId);
            if (metadata != null) {
                clientMetadataCache.put(clientId, metadata);
            }
        }
        return metadata != null ? metadata : new HashMap<>();
    }
    
    /**
     * Search for similar embeddings using cosine similarity
     */
    public List<SimilarityResult> findSimilarEmbeddings(String clientId, List<Double> queryEmbedding, int topK) {
        Map<String, List<Double>> clientEmbeddings = getClientEmbeddings(clientId);
        Map<String, Map<String, Object>> clientMetadata = getClientMetadata(clientId);
        
        List<SimilarityResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<Double>> entry : clientEmbeddings.entrySet()) {
            String documentId = entry.getKey();
            List<Double> embedding = entry.getValue();
            
            double similarity = calculateCosineSimilarity(queryEmbedding, embedding);
            Map<String, Object> metadata = clientMetadata.getOrDefault(documentId, new HashMap<>());
            
            results.add(new SimilarityResult(documentId, similarity, metadata));
        }
        
        // Sort by similarity and return top K
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return results.subList(0, Math.min(topK, results.size()));
    }
    
    /**
     * Delete embeddings for a specific client
     */
    public void deleteClientEmbeddings(String clientId) {
        try {
            // Remove from cache
            clientEmbeddingsCache.remove(clientId);
            clientMetadataCache.remove(clientId);
            
            // Delete files
            Path embeddingsFile = Paths.get(VECTOR_STORE_DIR, clientId + EMBEDDINGS_SUFFIX);
            Path metadataFile = Paths.get(VECTOR_STORE_DIR, clientId + METADATA_SUFFIX);
            
            Files.deleteIfExists(embeddingsFile);
            Files.deleteIfExists(metadataFile);
            
            System.out.println("✅ Deleted embeddings for client: " + clientId);
            
        } catch (IOException e) {
            System.err.println("❌ Error deleting embeddings for client " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get statistics for all clients
     */
    public Map<String, VectorStoreStats> getAllClientStats() {
        Map<String, VectorStoreStats> stats = new HashMap<>();
        
        // Check both cache and file system
        Set<String> allClients = new HashSet<>(clientEmbeddingsCache.keySet());
        
        try {
            Files.list(Paths.get(VECTOR_STORE_DIR))
                .filter(path -> path.toString().endsWith(EMBEDDINGS_SUFFIX))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String clientId = fileName.substring(0, fileName.length() - EMBEDDINGS_SUFFIX.length());
                    allClients.add(clientId);
                });
        } catch (IOException e) {
            System.err.println("❌ Error reading vector store directory: " + e.getMessage());
        }
        
        for (String clientId : allClients) {
            Map<String, List<Double>> embeddings = getClientEmbeddings(clientId);
            stats.put(clientId, new VectorStoreStats(clientId, embeddings.size()));
        }
        
        return stats;
    }
    
    /**
     * Persist client embeddings to file system
     */
    private void persistClientEmbeddings(String clientId) throws IOException {
        Map<String, List<Double>> embeddings = clientEmbeddingsCache.get(clientId);
        if (embeddings == null) return;
        
        try {
            JSONObject embeddingsJson = new JSONObject();
            for (Map.Entry<String, List<Double>> entry : embeddings.entrySet()) {
                JSONArray embeddingArray = new JSONArray(entry.getValue());
                embeddingsJson.put(entry.getKey(), embeddingArray);
            }
            
            Path filePath = Paths.get(VECTOR_STORE_DIR, clientId + EMBEDDINGS_SUFFIX);
            Files.write(filePath, embeddingsJson.toString(2).getBytes());
        } catch (JSONException e) {
            throw new IOException("Error creating JSON for embeddings", e);
        }
    }
    
    /**
     * Persist client metadata to file system
     */
    private void persistClientMetadata(String clientId) throws IOException {
        Map<String, Map<String, Object>> metadata = clientMetadataCache.get(clientId);
        if (metadata == null) return;
        
        try {
            JSONObject metadataJson = new JSONObject();
            for (Map.Entry<String, Map<String, Object>> entry : metadata.entrySet()) {
                JSONObject docMetadata = new JSONObject(entry.getValue());
                metadataJson.put(entry.getKey(), docMetadata);
            }
            
            Path filePath = Paths.get(VECTOR_STORE_DIR, clientId + METADATA_SUFFIX);
            Files.write(filePath, metadataJson.toString(2).getBytes());
        } catch (JSONException e) {
            throw new IOException("Error creating JSON for metadata", e);
        }
    }
    
    /**
     * Load client embeddings from file system
     */
    private Map<String, List<Double>> loadClientEmbeddings(String clientId) {
        try {
            Path filePath = Paths.get(VECTOR_STORE_DIR, clientId + EMBEDDINGS_SUFFIX);
            if (!Files.exists(filePath)) {
                return null;
            }
            
            String content = new String(Files.readAllBytes(filePath));
            JSONObject embeddingsJson = new JSONObject(content);
            
            Map<String, List<Double>> embeddings = new HashMap<>();
            Iterator<String> keys = embeddingsJson.keys();
            while (keys.hasNext()) {
                String documentId = keys.next();
                JSONArray embeddingArray = embeddingsJson.getJSONArray(documentId);
                List<Double> embedding = new ArrayList<>();
                for (int i = 0; i < embeddingArray.length(); i++) {
                    embedding.add(embeddingArray.getDouble(i));
                }
                embeddings.put(documentId, embedding);
            }
            
            return embeddings;
            
        } catch (Exception e) {
            System.err.println("❌ Error loading embeddings for client " + clientId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Load client metadata from file system
     */
    private Map<String, Map<String, Object>> loadClientMetadata(String clientId) {
        try {
            Path filePath = Paths.get(VECTOR_STORE_DIR, clientId + METADATA_SUFFIX);
            if (!Files.exists(filePath)) {
                return null;
            }
            
            String content = new String(Files.readAllBytes(filePath));
            JSONObject metadataJson = new JSONObject(content);
            
            Map<String, Map<String, Object>> metadata = new HashMap<>();
            Iterator<String> docKeys = metadataJson.keys();
            while (docKeys.hasNext()) {
                String documentId = docKeys.next();
                JSONObject docMetadata = metadataJson.getJSONObject(documentId);
                Map<String, Object> docMetadataMap = new HashMap<>();
                Iterator<String> fieldKeys = docMetadata.keys();
                while (fieldKeys.hasNext()) {
                    String key = fieldKeys.next();
                    docMetadataMap.put(key, docMetadata.get(key));
                }
                metadata.put(documentId, docMetadataMap);
            }
            
            return metadata;
            
        } catch (Exception e) {
            System.err.println("❌ Error loading metadata for client " + clientId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Data class for similarity search results
     */
    public static class SimilarityResult {
        private final String documentId;
        private final double similarity;
        private final Map<String, Object> metadata;
        
        public SimilarityResult(String documentId, double similarity, Map<String, Object> metadata) {
            this.documentId = documentId;
            this.similarity = similarity;
            this.metadata = metadata;
        }
        
        public String getDocumentId() { return documentId; }
        public double getSimilarity() { return similarity; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Data class for vector store statistics
     */
    public static class VectorStoreStats {
        private final String clientId;
        private final int embeddingCount;
        
        public VectorStoreStats(String clientId, int embeddingCount) {
            this.clientId = clientId;
            this.embeddingCount = embeddingCount;
        }
        
        public String getClientId() { return clientId; }
        public int getEmbeddingCount() { return embeddingCount; }
    }
} 