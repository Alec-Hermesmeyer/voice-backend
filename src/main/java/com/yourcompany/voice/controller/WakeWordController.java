package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.SimpleWakeWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wake-word-v2")
@CrossOrigin("*")
public class WakeWordController {

    // CHANGE THIS LINE: Replace CustomWakeWordService with SimpleWakeWordService
    @Autowired
    private SimpleWakeWordService wakeWordService;

    /**
     * Get all trained wake words
     */
    @GetMapping
    public ResponseEntity<?> getWakeWords() {
        try {
            String[] wakeWords = wakeWordService.getTrainedWakeWords();

            Map<String, Object> response = new HashMap<>();
            response.put("wakeWords", wakeWords);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve wake words: " + e.getMessage()));
        }
    }

    /**
     * Train a new wake word from an audio file
     */
    @PostMapping("/train-with-agent")
    public ResponseEntity<?> trainWakeWord(
            @RequestParam("name") String wakeWordName,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "agentId", required = false) String agentId) {

        try {
            // Validate inputs
            if (wakeWordName == null || wakeWordName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Wake word name is required"));
            }

            if (audioFile == null || audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Audio file is required"));
            }

            // Create a temporary file to store the uploaded audio
            Path tempFile = Files.createTempFile("wake-word-", ".wav");
            audioFile.transferTo(tempFile.toFile());

            // Train the wake word
            wakeWordService.trainWakeWord(wakeWordName, tempFile.toFile(), agentId);

            // Clean up the temporary file
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Wake word '" + wakeWordName + "' trained successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to train wake word: " + e.getMessage()));
        }
    }

    /**
     * Map a wake word to a specific agent
     */
    @PostMapping("/map-agent")
    public ResponseEntity<?> mapWakeWordToAgent(
            @RequestParam("wakeWord") String wakeWord,
            @RequestParam("agentId") String agentId) {

        try {
            // Validate inputs
            if (wakeWord == null || wakeWord.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Wake word name is required"));
            }

            if (agentId == null || agentId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Agent ID is required"));
            }

            // Check if wake word exists
            boolean wakeWordExists = false;
            for (String existingWakeWord : wakeWordService.getTrainedWakeWords()) {
                if (existingWakeWord.equals(wakeWord)) {
                    wakeWordExists = true;
                    break;
                }
            }

            if (!wakeWordExists) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Wake word '" + wakeWord + "' does not exist"));
            }

            // Map wake word to agent
            wakeWordService.mapWakeWordToAgent(wakeWord, agentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Wake word '" + wakeWord + "' mapped to agent '" + agentId + "'"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to map wake word to agent: " + e.getMessage()));
        }
    }

    /**
     * Get the agent associated with a wake word
     */
    @GetMapping("/agent/{wakeWord}")
    public ResponseEntity<?> getAgentForWakeWord(@PathVariable String wakeWord) {
        try {
            String agentId = wakeWordService.getAgentForWakeWord(wakeWord);

            if (agentId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No agent mapped to wake word '" + wakeWord + "'"));
            }

            return ResponseEntity.ok(Map.of(
                    "wakeWord", wakeWord,
                    "agentId", agentId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get agent for wake word: " + e.getMessage()));
        }
    }

    /**
     * ADD THIS NEW METHOD: Trigger a simulated wake word detection
     */
    @PostMapping("/test-wake-word")
    public ResponseEntity<?> testWakeWord(
            @RequestParam("wakeWord") String wakeWord,
            @RequestParam(value = "agentId", required = false) String agentId) {

        try {
            if (agentId != null && !agentId.isEmpty()) {
                // Map the wake word to the specified agent first
                wakeWordService.mapWakeWordToAgent(wakeWord, agentId);
            }

            // Trigger the simulated detection
            wakeWordService.simulateWakeWordDetection(wakeWord);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Simulated wake word detection for: " + wakeWord +
                            (agentId != null ? " with agent: " + agentId : "")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to simulate detection: " + e.getMessage()));
        }
    }
}