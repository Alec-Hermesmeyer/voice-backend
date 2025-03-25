package com.yourcompany.voice.service;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import com.yourcompany.voice.controller.WakeWordWebSocketHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sound.sampled.*;

@Service
public class WakeWordService {
    private Porcupine porcupine;
    private volatile boolean running = true;
    private TargetDataLine microphone;

    @PostConstruct
    public void initialize() {
        new Thread(this::startWakeWordDetection).start();
    }

    private void startWakeWordDetection() {
        try {
            String accessKey = "65WYUVQ+F5PljJBt9XX4uv/p7BPNqbGdAik1jEvchlBZLhCIml9ymw=="; // Replace with your Picovoice access key

            porcupine = new Porcupine.Builder()
                    .setAccessKey(accessKey)
                    .setBuiltInKeywords(new Porcupine.BuiltInKeyword[]{
                            Porcupine.BuiltInKeyword.PICOVOICE,
                            Porcupine.BuiltInKeyword.BUMBLEBEE
                    })
                    .build();

            initMicrophone();

            System.out.println("🎤 Wake word detection started...");

            while (running) {
                short[] audioFrame = getAudioFrame();
                int keywordIndex = porcupine.process(audioFrame);

                if (keywordIndex == 0) {
                    System.out.println("🔊 Detected wake word: Picovoice!");
                    WakeWordWebSocketHandler.broadcastWakeWord("Picovoice");
                } else if (keywordIndex == 1) {
                    System.out.println("🔊 Detected wake word: Bumblebee!");
                    WakeWordWebSocketHandler.broadcastWakeWord("Bumblebee");
                }
            }
        } catch (PorcupineException e) {
            e.printStackTrace();
        }
    }

    private void initMicrophone() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Microphone not supported.");
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private short[] getAudioFrame() {
        int frameLength = porcupine.getFrameLength();
        byte[] audioBuffer = new byte[frameLength * 2];

        if (microphone.read(audioBuffer, 0, audioBuffer.length) != audioBuffer.length) {
            System.err.println("⚠ Warning: Could not read full audio frame.");
        }

        short[] audioFrame = new short[frameLength];
        for (int i = 0; i < frameLength; i++) {
            audioFrame[i] = (short) ((audioBuffer[2 * i] & 0xFF) | (audioBuffer[2 * i + 1] << 8));
        }
        return audioFrame;
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (porcupine != null) {
            porcupine.delete();
        }
        System.out.println("🛑 Wake word detection stopped.");
    }
}
