package com.yourcompany.voice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import java.io.File;
import java.util.logging.Logger;

@SpringBootApplication
public class Application {
    
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    
    public static void main(String[] args) {
        logger.info("🚀 Starting Voice Backend Application...");
        
        // Log environment info
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Spring Profile: " + System.getProperty("spring.profiles.active", "default"));
        logger.info("Port: " + System.getenv("PORT"));
        
        // Create necessary directories early
        createRequiredDirectories();
        
        try {
            SpringApplication app = new SpringApplication(Application.class);
            app.run(args);
            logger.info("✅ Voice Backend Application started successfully!");
        } catch (Exception e) {
            logger.severe("❌ Failed to start Voice Backend Application: " + e.getMessage());
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
                        logger.info("📁 Created directory: " + dir);
                    }
                } else {
                    logger.info("📁 Directory already exists: " + dir);
                }
            }
        } catch (Exception e) {
            logger.warning("⚠️ Could not create directories: " + e.getMessage());
        }
    }
    
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        logger.info("🌟 Application context loaded and started!");
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("🎉 === Voice Backend Application is READY ===");
        
        // Log current configuration
        String port = System.getProperty("server.port", System.getenv("PORT"));
        if (port == null) port = "8080";
        String profile = System.getProperty("spring.profiles.active", "default");
        
        logger.info("🌐 Server port: " + port);
        logger.info("🔧 Active profile: " + profile);
        logger.info("❤️ Health check URL: http://localhost:" + port + "/health");
        logger.info("🔗 API endpoints available at: http://localhost:" + port + "/api/");
        
        // Test that health endpoint is accessible
        try {
            java.net.URL url = new java.net.URL("http://localhost:" + port + "/health");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            logger.info("✅ Health endpoint test: " + responseCode + " " + conn.getResponseMessage());
            
        } catch (Exception e) {
            logger.warning("⚠️ Could not test health endpoint: " + e.getMessage());
        }
        
        logger.info("🎯 Deployment successful - Application is ready to serve requests!");
    }
}
