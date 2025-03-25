package com.yourcompany.voice.controller;

import com.yourcompany.voice.service.ElevenLabsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class TTSController {

    @Autowired
    private ElevenLabsService elevenLabsService;

    @PostMapping("/tts")
    public ResponseEntity<byte[]> getTtsAudio(@RequestBody TextRequest request) {
        try {
            byte[] audioBytes = elevenLabsService.getSpeechAudio(request.getText());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("❌ Error in TTSController: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class TextRequest {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}

