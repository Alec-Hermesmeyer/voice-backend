package com.yourcompany.voice.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class DeepgramService {

    private static final String DEEPGRAM_API_URL =
            "wss://api.deepgram.com/v1/listen?encoding=linear16&sample_rate=16000&model=nova&smart_format=true";

    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${deepgram.api.key}")
    private String deepgramApiKey;

    public String transcribe(byte[] audioData) throws IOException {
        Request request = new Request.Builder()
                .url(DEEPGRAM_API_URL)
                .header("Authorization", "Token " + deepgramApiKey)
                .post(RequestBody.create(audioData, MediaType.parse("audio/wav")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Deepgram API error: " + response.code() + " - " + response.body().string());
            }

            return response.body().string();
        }
    }
}
