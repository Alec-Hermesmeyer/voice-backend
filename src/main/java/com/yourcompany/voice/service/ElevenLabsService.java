package com.yourcompany.voice.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.file.*;

@Service
public class ElevenLabsService {
    @Value("${elevenlabs.api.key}")
    private String elevenLabsApiKey;

    private final OkHttpClient client = new OkHttpClient();

    private static final String ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1/text-to-speech/21m00Tcm4TlvDq8ikWAM";
    private static final String VOICE_ID = "YOUR_VOICE_ID";
    private static final String CACHE_DIR = "tts-cache";

    public byte[] getSpeechAudio(String text) throws IOException {
        String cacheKey = generateHash(text);
        Path cachePath = Paths.get(CACHE_DIR, cacheKey + ".mp3");

        try {
            if (Files.exists(cachePath)) {
                System.out.println("🎧 Playing cached audio for text: " + text);
                return Files.readAllBytes(cachePath);
            }

            System.out.println("🎧 Requesting audio from ElevenLabs for text: " + text);

            RequestBody requestBody = RequestBody.create(
                    "{\"text\": \"" + text.replace("\"", "\\\"") + "\"}",
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(ELEVENLABS_API_URL.replace("{voice_id}", VOICE_ID))
                    .post(requestBody)
                    .addHeader("xi-api-key", elevenLabsApiKey)
                    .addHeader("accept", "audio/mpeg")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("❌ TTS request failed: " + response.code() + " - " + response.message());
                    System.err.println("❌ Response body: " + response.body().string());
                    throw new IOException("TTS request failed: " + response.code());
                }

                if (response.body() == null) {
                    System.err.println("❌ TTS response body is null");
                    throw new IOException("Empty TTS response body");
                }

                byte[] audioBytes = response.body().bytes();

                Files.createDirectories(cachePath.getParent());
                Files.write(cachePath, audioBytes);

                return audioBytes;
            }
        } catch (Exception e) {
            System.err.println("🚨 Exception in getSpeechAudio: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String generateHash(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes()).substring(0, 10);
    }
}
