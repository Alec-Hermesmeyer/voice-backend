package com.yourcompany.voice.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class DeepgramService {

    private static final String DEEPGRAM_API_URL = 
            "https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true";

    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${deepgram.api.key}")
    private String deepgramApiKey;

    public String transcribe(byte[] audioData) throws IOException {
        RequestBody requestBody = RequestBody.create(audioData, MediaType.parse("audio/wav"));
        
        Request request = new Request.Builder()
                .url(DEEPGRAM_API_URL)
                .header("Authorization", "Token " + deepgramApiKey)
                .header("Content-Type", "audio/wav")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Deepgram API error: " + response.code() + " - " + response.body().string());
            }

            return response.body().string();
        }
    }
}
