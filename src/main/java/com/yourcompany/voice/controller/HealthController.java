package com.yourcompany.voice.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Health Check Controller with no dependencies
 */
@RestController
public class HealthController {

    private final long startupTime = System.currentTimeMillis();

    @GetMapping("/api/voice-interaction/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("service", "voice-backend");
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("startupTime", startupTime);
            healthInfo.put("uptime", System.currentTimeMillis() - startupTime);
            healthInfo.put("message", "Service is running");
            healthInfo.put("version", "1.0-SNAPSHOT");
            
            // Basic memory info
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("total", runtime.totalMemory());
            memory.put("free", runtime.freeMemory());
            memory.put("used", runtime.totalMemory() - runtime.freeMemory());
            memory.put("max", runtime.maxMemory());
            healthInfo.put("memory", memory);
            
            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("status", "DOWN");
            errorInfo.put("service", "voice-backend");
            errorInfo.put("timestamp", System.currentTimeMillis());
            errorInfo.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorInfo);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthSimple() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "voice-backend");
        info.put("status", "running");
        info.put("timestamp", Instant.now().toString());
        info.put("message", "Voice Backend API is running");
        return ResponseEntity.ok(info);
    }
} 