//package com.yourcompany.voice.controller;
//
//import com.yourcompany.voice.controller.WakeWordWebSocketHandler;
//import com.yourcompany.voice.service.SimpleWakeWordService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import javax.sound.sampled.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/audio-diagnostics")
//public class AudioDiagnosticController {
//
//    @Autowired
//    private SimpleWakeWordService wakeWordService;
//
//    /**
//     * Test endpoint to trigger a wake word detection message
//     */
//    @GetMapping("/test-wake-word")
//    public ResponseEntity<?> testWakeWordDetection(@RequestParam(defaultValue = "TestWord") String wakeWord,
//                                                   @RequestParam(required = false) String agentId) {
//        try {
//            // Simulate a wake word detection
//            System.out.println("🔍 Simulating wake word detection for: " + wakeWord);
//
//            // Use the WebSocketHandler to broadcast a wake word detection
//            if (agentId != null) {
//                WakeWordWebSocketHandler.broadcastWakeWordDetection(wakeWord, agentId, 0.95);
//            } else {
//                WakeWordWebSocketHandler.broadcastWakeWord(wakeWord);
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Simulated wake word detection for: " + wakeWord
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "success", false,
//                    "error", "Failed to simulate wake word detection: " + e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Endpoint to get detailed audio information about available microphones
//     */
//    @GetMapping("/microphones")
//    public ResponseEntity<?> getMicrophoneInfo() {
//        List<Map<String, Object>> microphoneInfoList = new ArrayList<>();
//
//        try {
//            // Get all mixer info objects
//            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
//            System.out.println("Found " + mixerInfos.length + " audio devices");
//
//            for (Mixer.Info mixerInfo : mixerInfos) {
//                Map<String, Object> micInfo = new HashMap<>();
//                micInfo.put("name", mixerInfo.getName());
//                micInfo.put("description", mixerInfo.getDescription());
//                micInfo.put("vendor", mixerInfo.getVendor());
//                micInfo.put("version", mixerInfo.getVersion());
//
//                // Get the actual mixer
//                Mixer mixer = AudioSystem.getMixer(mixerInfo);
//
//                // Check if this is a capture device (has target lines)
//                Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
//                boolean isInputDevice = false;
//
//                List<Map<String, Object>> lineList = new ArrayList<>();
//                for (Line.Info lineInfo : targetLineInfos) {
//                    Map<String, Object> lineInfoMap = new HashMap<>();
//                    lineInfoMap.put("lineType", lineInfo.getClass().getSimpleName());
//                    lineInfoMap.put("lineDescription", lineInfo.toString());
//
//                    if (lineInfo instanceof DataLine.Info) {
//                        isInputDevice = true;
//                        DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
//
//                        // Check supported formats
//                        AudioFormat[] formats = dataLineInfo.getFormats();
//                        List<Map<String, Object>> formatList = new ArrayList<>();
//
//                        for (AudioFormat format : formats) {
//                            Map<String, Object> formatMap = new HashMap<>();
//                            formatMap.put("sampleRate", format.getSampleRate());
//                            formatMap.put("sampleSizeInBits", format.getSampleSizeInBits());
//                            formatMap.put("channels", format.getChannels());
//                            formatMap.put("frameSize", format.getFrameSize());
//                            formatMap.put("frameRate", format.getFrameRate());
//                            formatMap.put("encoding", format.getEncoding().toString());
//                            formatMap.put("bigEndian", format.isBigEndian());
//
//                            formatList.add(formatMap);
//                        }
//
//                        lineInfoMap.put("supportedFormats", formatList);
//                    }
//
//                    lineList.add(lineInfoMap);
//                }
//
//                micInfo.put("isInputDevice", isInputDevice);
//                micInfo.put("lines", lineList);
//
//                if (isInputDevice) {
//                    microphoneInfoList.add(micInfo);
//                }
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "microphones", microphoneInfoList,
//                    "count", microphoneInfoList.size()
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "success", false,
//                    "error", "Failed to get microphone information: " + e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Endpoint to test audio levels from the microphone
//     */
//    @GetMapping("/test-audio-levels")
//    public ResponseEntity<?> testAudioLevels(@RequestParam(defaultValue = "3") int seconds) {
//        try {
//            // Limit test duration to reasonable values
//            if (seconds < 1) seconds = 1;
//            if (seconds > 10) seconds = 10;
//
//            System.out.println("🎤 Testing audio levels for " + seconds + " seconds...");
//
//            // Format for capture
//            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
//            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//
//            if (!AudioSystem.isLineSupported(info)) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "error", "Default microphone format not supported: " + format
//                ));
//            }
//
//            // Open microphone
//            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
//            line.open(format);
//            line.start();
//
//            System.out.println("🎙 Microphone opened successfully");
//
//            // Buffer for storing audio data
//            byte[] buffer = new byte[4096];
//
//            // Calculate audio levels for the specified duration
//            List<Double> audioLevels = new ArrayList<>();
//            long endTime = System.currentTimeMillis() + (seconds * 1000);
//
//            while (System.currentTimeMillis() < endTime) {
//                int bytesRead = line.read(buffer, 0, buffer.length);
//
//                if (bytesRead > 0) {
//                    // Calculate RMS value for this buffer
//                    double sum = 0;
//                    for (int i = 0; i < bytesRead; i += 2) {
//                        short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
//                        sum += sample * sample;
//                    }
//                    double rms = Math.sqrt(sum / (bytesRead / 2));
//                    audioLevels.add(rms);
//                }
//
//                // Short sleep to prevent CPU hogging
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//
//            // Clean up
//            line.stop();
//            line.close();
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Audio level test completed",
//                    "durationSeconds", seconds,
//                    "samples", audioLevels.size(),
//                    "audioLevels", audioLevels,
//                    "averageLevel", audioLevels.stream().mapToDouble(Double::doubleValue).average().orElse(0),
//                    "maxLevel", audioLevels.stream().mapToDouble(Double::doubleValue).max().orElse(0)
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "success", false,
//                    "error", "Failed to test audio levels: " + e.getMessage(),
//                    "details", e.toString()
//            ));
//        }
//    }
//
//    /**
//     * Force restart the wake word detection service
//     */
//    @PostMapping("/restart-detection")
//    public ResponseEntity<?> restartWakeWordDetection() {
//        try {
//            wakeWordService.restartDetection();
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Wake word detection restarted"
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "success", false,
//                    "error", "Failed to restart wake word detection: " + e.getMessage()
//            ));
//        }
//    }
//}