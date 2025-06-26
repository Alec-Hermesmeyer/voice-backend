package com.yourcompany.voice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.util.logging.Logger;

@SpringBootApplication
public class Application {
    
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    
    public static void main(String[] args) {
        logger.info("Starting Voice Backend Application...");
        
        // Create necessary directories early
        createRequiredDirectories();
        
        try {
            SpringApplication.run(Application.class, args);
            logger.info("Voice Backend Application started successfully!");
        } catch (Exception e) {
            logger.severe("Failed to start Voice Backend Application: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create required directories for the application
     */
    private static void createRequiredDirectories() {
        try {
            String[] directories = {"vector-store", "user-data", "tts-cache"};
            
            for (String dir : directories) {
                File directory = new File(dir);
                if (!directory.exists()) {
                    boolean created = directory.mkdirs();
                    if (created) {
                        logger.info("Created directory: " + dir);
                    }
                } else {
                    logger.info("Directory already exists: " + dir);
                }
            }
        } catch (Exception e) {
            logger.warning("Could not create directories: " + e.getMessage());
        }
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("=== Voice Backend Application is READY ===");
        logger.info("Health endpoint available at: /api/voice-interaction/health");
        
        // Log current configuration
        String port = System.getProperty("server.port", "8080");
        String profile = System.getProperty("spring.profiles.active", "default");
        
        logger.info("Server port: " + port);
        logger.info("Active profile: " + profile);
    }
}
