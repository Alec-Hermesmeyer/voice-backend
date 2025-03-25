package com.yourcompany.voice.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class WhisperService {

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public String transcribe(byte[] audioData) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        "audio.wav",
                        RequestBody.create(audioData, MediaType.parse("audio/wav"))
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "json")
                .addFormDataPart("language", "en")
                .build();

        Request request = new Request.Builder()
                .url(WHISPER_API_URL)
                .header("Authorization", "Bearer " + openAiApiKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Whisper API error: " + response.code() + " - " + response.body().string());
            }

            return response.body().string();
        }
    }
}
