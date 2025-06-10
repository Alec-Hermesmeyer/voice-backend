package com.yourcompany.voice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Health Check Controller with no dependencies
 * Ensures health endpoint is always available regardless of service startup issues
 */
@RestController
@RequestMapping("/api/voice-interaction")
@CrossOrigin(origins = "*")
public class HealthController {

    /**
     * Basic health check endpoint with no service dependencies
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "voice-backend");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Service is running");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Additional basic info endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "voice-backend");
        response.put("version", "1.0.0");
        response.put("java.version", System.getProperty("java.version"));
        response.put("spring.profiles.active", System.getProperty("spring.profiles.active", "default"));
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
} 