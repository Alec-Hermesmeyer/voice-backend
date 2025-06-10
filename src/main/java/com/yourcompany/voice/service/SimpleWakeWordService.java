package com.yourcompany.voice.service;

import com.yourcompany.voice.controller.WakeWordWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wake word detection service that listens for actual audio input
 * and tries to detect wake words.
 */
@Service
public class SimpleWakeWordService {

    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final int FRAME_SIZE = 512;

    // Detection thresholds - configurable for different environments
    private static final double ENERGY_THRESHOLD = 500.0;
    private static final double WAKE_WORD_ENERGY_MULTIPLIER = 1.5;
    private static final double SILENCE_THRESHOLD = 200.0;

    // Timing configuration
    private static final long DETECTION_COOLDOWN_MS = 2000;
    private static final int SILENCE_FRAMES_FOR_END = 20; // ~400ms of silence
    private static final int MIN_SPEECH_FRAMES = 10; // Minimum frames for valid speech

    // Audio history for pattern matching
    private static final int AUDIO_HISTORY_SECONDS = 3;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private TargetDataLine microphone;
    private Thread detectionThread;
    private long lastDetectionTime = 0;
    
    // Production environment flag
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    private boolean isProduction;
    private boolean audioAvailable = false;

    // Wake word storage with confidence patterns
    private final Map<String, WakeWordPattern> wakeWordPatterns = new ConcurrentHashMap<>();
    private final Map<String, String> wakeWordHandlers = new ConcurrentHashMap<>();

    // Audio processing components
    private RingBuffer audioHistory;
    private AudioProcessor audioProcessor;

    @PostConstruct
    public void initialize() {
        isProduction = "production".equals(activeProfile);
        
        try {
            initializeDefaultWakeWords();
            audioHistory = new RingBuffer(SAMPLE_RATE * AUDIO_HISTORY_SECONDS);
            audioProcessor = new AudioProcessor();

            // Only try to start detection if not in production or if audio is available
            if (!isProduction) {
                startDetectionService();
                System.out.println("✅ Wake word detection service initialized successfully");
            } else {
                System.out.println("ℹ️ Production mode: Wake word detection service initialized without audio (audio hardware not available)");
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize wake word service: " + e.getMessage());
            if (!isProduction) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize with common wake words
     */
    private void initializeDefaultWakeWords() {
        // Traditional wake words with basic patterns
        registerWakeWord("computer", "default", createBasicPattern("computer"));
        registerWakeWord("assistant", "default", createBasicPattern("assistant"));
        registerWakeWord("wake up", "default", createBasicPattern("wake up"));
        registerWakeWord("hey there", "default", createBasicPattern("hey there"));
        registerWakeWord("listen", "default", createBasicPattern("listen"));
    }

    /**
     * Register a new wake word with its handler
     */
    public void registerWakeWord(String wakeWord, String handlerId, WakeWordPattern pattern) {
        wakeWordPatterns.put(wakeWord.toLowerCase(), pattern);
        wakeWordHandlers.put(wakeWord.toLowerCase(), handlerId);
        System.out.println("✅ Registered wake word: '" + wakeWord + "' -> handler: '" + handlerId + "'");
    }

    /**
     * Train a wake word from audio samples
     */
    public void trainWakeWord(String wakeWordName, java.io.File audioFile, String handlerId) {
        try {
            System.out.println("📝 Training wake word: " + wakeWordName);

            // Process audio file to create pattern
            WakeWordPattern pattern = audioProcessor.analyzeAudioFile(audioFile);
            registerWakeWord(wakeWordName, handlerId != null ? handlerId : "default", pattern);

            System.out.println("✅ Successfully trained wake word: " + wakeWordName);
            WakeWordWebSocketHandler.broadcastWakeWordTrained(wakeWordName);

        } catch (Exception e) {
            System.err.println("❌ Error training wake word '" + wakeWordName + "': " + e.getMessage());
            WakeWordWebSocketHandler.broadcastError("Failed to train wake word: " + e.getMessage());
        }
    }

    /**
     * Get all registered wake words
     */
    public String[] getRegisteredWakeWords() {
        return wakeWordPatterns.keySet().toArray(new String[0]);
    }

    /**
     * Get all trained wake words (alias for backward compatibility)
     */
    public String[] getTrainedWakeWords() {
        return getRegisteredWakeWords();
    }

    /**
     * Get handler for a specific wake word
     */
    public String getWakeWordHandler(String wakeWord) {
        return wakeWordHandlers.get(wakeWord.toLowerCase());
    }

    /**
     * Get agent for wake word (alias for backward compatibility)
     */
    public String getAgentForWakeWord(String wakeWord) {
        return getWakeWordHandler(wakeWord);
    }

    /**
     * Map wake word to agent (alias for backward compatibility)
     */
    public void mapWakeWordToAgent(String wakeWord, String agentId) {
        registerWakeWord(wakeWord, agentId, createBasicPattern(wakeWord));
    }

    /**
     * Start the detection service
     */
    public void startDetectionService() {
        if (running.get()) {
            System.out.println("⚠️ Detection service already running");
            return;
        }

        // Skip audio detection in production environments
        if (isProduction) {
            System.out.println("ℹ️ Production mode: Skipping audio detection (no audio hardware available)");
            return;
        }

        running.set(true);
        detectionThread = new Thread(this::runDetectionLoop);
        detectionThread.setDaemon(true);
        detectionThread.setName("WakeWordDetectionThread");
        detectionThread.start();

        System.out.println("🎤 Wake word detection service started");
    }

    /**
     * Stop the detection service
     */
    public void stopDetectionService() {
        System.out.println("⏹️ Stopping wake word detection service...");
        running.set(false);

        if (detectionThread != null && detectionThread.isAlive()) {
            try {
                detectionThread.interrupt();
                detectionThread.join(2000);
            } catch (InterruptedException e) {
                System.err.println("⚠️ Interrupted while stopping detection thread");
                Thread.currentThread().interrupt();
            }
        }

        closeMicrophone();
        System.out.println("⏹️ Wake word detection service stopped");
    }

    /**
     * Restart the detection service
     */
    public void restartDetectionService() {
        System.out.println("🔄 Restarting wake word detection service...");
        stopDetectionService();

        try {
            Thread.sleep(1000); // Brief pause before restart
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        startDetectionService();
        System.out.println("✅ Wake word detection service restarted");
    }

    /**
     * Restart detection (alias for backward compatibility)
     */
    public void restartDetection() {
        restartDetectionService();
    }

    /**
     * Main detection loop
     */
    private void runDetectionLoop() {
        System.out.println("🎤 Starting wake word detection loop...");

        try {
            initializeMicrophone();

            if (microphone == null) {
                System.err.println("❌ Cannot start detection - microphone initialization failed");
                return;
            }

            System.out.println("🔊 Listening for wake words: " + Arrays.toString(getRegisteredWakeWords()));

            byte[] buffer = new byte[FRAME_SIZE * 2];
            short[] audioFrame = new short[FRAME_SIZE];

            SpeechDetector speechDetector = new SpeechDetector();

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);

                    if (bytesRead <= 0) {
                        Thread.sleep(10);
                        continue;
                    }

                    // Convert bytes to audio samples
                    convertBytesToSamples(buffer, audioFrame, bytesRead);

                    // Add to history
                    for (int i = 0; i < Math.min(FRAME_SIZE, bytesRead / 2); i++) {
                        audioHistory.add(audioFrame[i]);
                    }

                    // Process speech detection
                    SpeechEvent speechEvent = speechDetector.processSamples(audioFrame);

                    if (speechEvent == SpeechEvent.SPEECH_END) {
                        processUtterance(speechDetector.getMaxEnergy());
                    }

                    Thread.sleep(5); // Small delay to prevent excessive CPU usage

                } catch (InterruptedException e) {
                    System.out.println("🔇 Detection thread interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Error in detection loop: " + e.getMessage());
                    Thread.sleep(100); // Brief pause before continuing
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Fatal error in wake word detection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeMicrophone();
        }
    }

    /**
     * Process a complete utterance for wake word detection
     */
    private void processUtterance(double maxEnergy) {
        long currentTime = System.currentTimeMillis();

        // Check cooldown period
        if (currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS) {
            return;
        }

        System.out.println("🔍 Processing utterance (energy: " + String.format("%.1f", maxEnergy) + ")");

        // Get recent audio for analysis
        short[] recentAudio = audioHistory.getRecentSamples(SAMPLE_RATE * 2); // Last 2 seconds

        // Check against all registered wake words
        WakeWordMatch bestMatch = findBestWakeWordMatch(recentAudio, maxEnergy);

        if (bestMatch != null && bestMatch.confidence > 0.7) {
            handleWakeWordDetection(bestMatch);
            lastDetectionTime = currentTime;
        } else {
            System.out.println("👂 No wake word detected (best confidence: " +
                    (bestMatch != null ? String.format("%.2f", bestMatch.confidence) : "0.00") + ")");
        }
    }

    /**
     * Find the best matching wake word
     */
    private WakeWordMatch findBestWakeWordMatch(short[] audioSamples, double energy) {
        WakeWordMatch bestMatch = null;
        double bestConfidence = 0.0;

        for (Map.Entry<String, WakeWordPattern> entry : wakeWordPatterns.entrySet()) {
            String wakeWord = entry.getKey();
            WakeWordPattern pattern = entry.getValue();

            double confidence = audioProcessor.calculateMatchConfidence(audioSamples, pattern, energy);

            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestMatch = new WakeWordMatch(wakeWord, confidence);
            }
        }

        return bestMatch;
    }

    /**
     * Handle wake word detection
     */
    private void handleWakeWordDetection(WakeWordMatch match) {
        String handlerId = wakeWordHandlers.get(match.wakeWord);

        System.out.println("🔊 Wake word detected: '" + match.wakeWord +
                "' (confidence: " + String.format("%.2f", match.confidence) +
                ", handler: " + handlerId + ")");

        // Broadcast the detection
        WakeWordWebSocketHandler.broadcastWakeWordDetection(
                match.wakeWord, handlerId, match.confidence);
    }

    /**
     * Initialize microphone for audio capture
     */
    private void initializeMicrophone() {
        if (isProduction) {
            System.out.println("ℹ️ Production mode: Skipping microphone initialization");
            microphone = null;
            return;
        }
        
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio line not supported");
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format, FRAME_SIZE * 4); // Larger buffer
            microphone.start();

            System.out.println("🎙️ Microphone initialized (rate: " + SAMPLE_RATE + "Hz)");

        } catch (LineUnavailableException e) {
            System.err.println("❌ Microphone initialization failed: " + e.getMessage());
            microphone = null;
            if (!isProduction) {
                WakeWordWebSocketHandler.broadcastError("Microphone unavailable: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during microphone initialization: " + e.getMessage());
            microphone = null;
            if (!isProduction) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close microphone resources
     */
    private void closeMicrophone() {
        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
                microphone = null;
                System.out.println("🎙️ Microphone closed");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing microphone: " + e.getMessage());
            }
        }
    }

    /**
     * Convert byte array to short samples
     */
    private void convertBytesToSamples(byte[] buffer, short[] audioFrame, int bytesRead) {
        int samples = Math.min(FRAME_SIZE, bytesRead / 2);
        for (int i = 0; i < samples; i++) {
            audioFrame[i] = (short) ((buffer[2 * i] & 0xFF) | (buffer[2 * i + 1] << 8));
        }
    }

    /**
     * Create a basic pattern for simple wake word matching
     */
    private WakeWordPattern createBasicPattern(String word) {
        // This is a simplified pattern - in a real implementation,
        // you'd analyze actual audio samples to create acoustic patterns
        return new WakeWordPattern(word.length() * 100, word.length() * 50,
                estimateWordDuration(word));
    }

    /**
     * Estimate duration of a word in milliseconds
     */
    private int estimateWordDuration(String word) {
        // Rough estimate: 100ms per syllable/character group
        return Math.max(300, word.length() * 80);
    }

    /**
     * Manually trigger wake word detection (for testing)
     */
    public void simulateWakeWordDetection(String wakeWord) {
        String handlerId = wakeWordHandlers.getOrDefault(wakeWord.toLowerCase(), "default");
        System.out.println("🔊 Simulating wake word detection: " + wakeWord + " -> " + handlerId);
        WakeWordWebSocketHandler.broadcastWakeWordDetection(wakeWord, handlerId, 0.95);
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("🛑 Shutting down wake word detection service...");
        stopDetectionService();
    }

    // Supporting classes

    /**
     * Ring buffer for audio history
     */
    private static class RingBuffer {
        private final short[] buffer;
        private int writePosition = 0;
        private boolean full = false;

        public RingBuffer(int size) {
            this.buffer = new short[size];
        }

        public void add(short value) {
            buffer[writePosition] = value;
            writePosition = (writePosition + 1) % buffer.length;
            if (writePosition == 0) {
                full = true;
            }
        }

        public short[] getRecentSamples(int count) {
            if (!full && writePosition < count) {
                return Arrays.copyOf(buffer, writePosition);
            }

            count = Math.min(count, buffer.length);
            short[] result = new short[count];

            int start = (writePosition - count + buffer.length) % buffer.length;
            for (int i = 0; i < count; i++) {
                result[i] = buffer[(start + i) % buffer.length];
            }

            return result;
        }
    }

    /**
     * Speech detection state machine
     */
    private class SpeechDetector {
        private boolean inSpeech = false;
        private int silentFrames = 0;
        private int speechFrames = 0;
        private double maxEnergy = 0;

        public SpeechEvent processSamples(short[] samples) {
            double energy = calculateEnergy(samples);

            if (energy > ENERGY_THRESHOLD) {
                if (!inSpeech) {
                    inSpeech = true;
                    speechFrames = 1;
                    maxEnergy = energy;
                    silentFrames = 0;
                    return SpeechEvent.SPEECH_START;
                } else {
                    speechFrames++;
                    maxEnergy = Math.max(maxEnergy, energy);
                    silentFrames = 0;
                    return SpeechEvent.SPEECH_CONTINUE;
                }
            } else if (inSpeech) {
                silentFrames++;
                if (silentFrames >= SILENCE_FRAMES_FOR_END && speechFrames >= MIN_SPEECH_FRAMES) {
                    inSpeech = false;
                    return SpeechEvent.SPEECH_END;
                }
            }

            return SpeechEvent.SILENCE;
        }

        public double getMaxEnergy() {
            return maxEnergy;
        }

        private double calculateEnergy(short[] samples) {
            double sum = 0;
            for (short sample : samples) {
                sum += sample * sample;
            }
            return Math.sqrt(sum / samples.length);
        }
    }

    /**
     * Wake word pattern for matching
     */
    public static class WakeWordPattern {
        public final double expectedEnergy;
        public final double energyTolerance;
        public final int expectedDurationMs;

        public WakeWordPattern(double expectedEnergy, double energyTolerance, int expectedDurationMs) {
            this.expectedEnergy = expectedEnergy;
            this.energyTolerance = energyTolerance;
            this.expectedDurationMs = expectedDurationMs;
        }
    }

    /**
     * Wake word match result
     */
    private static class WakeWordMatch {
        public final String wakeWord;
        public final double confidence;

        public WakeWordMatch(String wakeWord, double confidence) {
            this.wakeWord = wakeWord;
            this.confidence = confidence;
        }
    }

    /**
     * Speech detection events
     */
    private enum SpeechEvent {
        SILENCE, SPEECH_START, SPEECH_CONTINUE, SPEECH_END
    }

    /**
     * Audio processing utilities
     */
    private static class AudioProcessor {

        public WakeWordPattern analyzeAudioFile(java.io.File audioFile) {
            // Placeholder for audio file analysis
            // In a real implementation, you'd process the audio file to extract
            // features like energy patterns, spectral characteristics, etc.
            String fileName = audioFile.getName().toLowerCase();
            return new WakeWordPattern(800.0, 400.0, estimateDurationFromFile(audioFile));
        }

        public double calculateMatchConfidence(short[] audioSamples, WakeWordPattern pattern, double energy) {
            // Simplified confidence calculation
            // In a real implementation, you'd use more sophisticated matching algorithms

            double energyMatch = 1.0 - Math.abs(energy - pattern.expectedEnergy) /
                    (pattern.expectedEnergy + pattern.energyTolerance);
            energyMatch = Math.max(0, Math.min(1, energyMatch));

            // Basic duration check (simplified)
            double durationScore = audioSamples.length > 8000 ? 0.8 : 0.4; // Rough check

            // Energy threshold check
            double thresholdScore = energy > ENERGY_THRESHOLD * WAKE_WORD_ENERGY_MULTIPLIER ? 0.9 : 0.1;

            return (energyMatch + durationScore + thresholdScore) / 3.0;
        }

        private int estimateDurationFromFile(java.io.File file) {
            // Rough estimate based on file size
            long fileSize = file.length();
            return (int) Math.max(300, Math.min(2000, fileSize / 100));
        }
    }
}