package com.yourcompany.voice.utility;

import com.yourcompany.voice.service.SimpleWakeWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class WakeWordRecorder {

    private static final int SAMPLE_RATE = 16000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final int MAX_RECORDING_SECONDS = 5;

    public File recordAudio() throws IOException {
        try {
            // Set up audio format
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);

            // Create temp file for the recording
            File outputFile = File.createTempFile("wake-word-recording-", ".wav");

            // Create recorder (simplified - in a real app you'd use a more robust solution)
            System.out.println("Starting recording for up to " + MAX_RECORDING_SECONDS + " seconds. Press Enter to stop.");

            // Here would be code to record audio and save it to the output file
            // This is a simplified example

            return outputFile;
        } catch (Exception e) {
            throw new IOException("Failed to record audio: " + e.getMessage(), e);
        }
    }

    public void saveRecording(byte[] audioData, String wakeWordName) throws IOException {
        try {
            // Convert byte array to audio format
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);

            // Create a file to store the recording
            File outputDir = new File("wake-word-samples");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File outputFile = new File(outputDir, wakeWordName + "-" + System.currentTimeMillis() + ".wav");

            // Create AudioInputStream from byte array
            AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(audioData),
                    format,
                    audioData.length / format.getFrameSize()
            );

            // Write to file
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);

            System.out.println("Saved recording to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new IOException("Failed to save recording: " + e.getMessage(), e);
        }
    }

    @RestController
    @RequestMapping("/api/wake-word")
    @CrossOrigin(origins = "*") // Allow cross-origin for development
    public static class WakeWordTrainerController {

        @Autowired
        private SimpleWakeWordService wakeWordService;

        @PostMapping("/train")
        public ResponseEntity<?> trainWakeWord(
                @RequestParam("name") String wakeWordName,
                @RequestParam("audio") MultipartFile audioFile,
                @RequestParam(value = "agentId", required = false) String agentId) {

            try {
                System.out.println("Received wake word training request for: " + wakeWordName +
                        (agentId != null ? " with agent: " + agentId : ""));

                // Create a temporary file to store the audio
                Path tempFile = Files.createTempFile("wake-word-", ".wav");
                try (var fos = Files.newOutputStream(tempFile)) {
                    fos.write(audioFile.getBytes());
                }

                // Train the wake word
                wakeWordService.trainWakeWord(wakeWordName, tempFile.toFile(), agentId);

                // Clean up the temp file
                Files.delete(tempFile);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Wake word '" + wakeWordName + "' trained successfully");

                return ResponseEntity.ok(response);

            } catch (IOException e) {
                e.printStackTrace();

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Error training wake word: " + e.getMessage());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }

        /**
         * List all available wake words
         */
        @GetMapping
        public ResponseEntity<?> getWakeWords() {
            try {
                String[] wakeWords = wakeWordService.getTrainedWakeWords();

                Map<String, Object> response = new HashMap<>();
                response.put("wakeWords", wakeWords);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to retrieve wake words: " + e.getMessage());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
         * For testing: trigger a simulated wake word detection
         */
        @PostMapping("/simulate-detection")
        public ResponseEntity<?> simulateWakeWordDetection(
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
}
