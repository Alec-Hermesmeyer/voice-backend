package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.CommandService;
import okhttp3.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SpeechToTextController {

    private static final String OPENAI_WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final OkHttpClient client = new OkHttpClient();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Autowired
    private CommandService commandService;

    @PostMapping("/speech-to-text")
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("engine") String engine,
            @RequestPart("audio") MultipartFile audioFile
    ) throws Exception {

        if ("whisper".equalsIgnoreCase(engine)) {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getOriginalFilename(),
                            RequestBody.create(audioFile.getBytes(), MediaType.parse("audio/wav")))
                    .addFormDataPart("model", "whisper-1")
                    .build();

            Request request = new Request.Builder()
                    .url(OPENAI_WHISPER_API_URL)
                    .addHeader("Authorization", "Bearer " + openAiApiKey)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return ResponseEntity.status(response.code()).body(response.body().string());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            String transcript = json.optString("text", responseBody);

            Map<String, String> result = new HashMap<>();
            result.put("transcript", transcript);

            Optional<String> detectedCommand = commandService.detectCommand(transcript);
            detectedCommand.ifPresent(cmd -> result.put("command", commandService.executeCommand(cmd)));

            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body("Invalid STT engine specified.");
    }
}
