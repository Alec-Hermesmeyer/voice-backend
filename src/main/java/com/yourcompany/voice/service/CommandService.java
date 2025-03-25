package com.yourcompany.voice.service;

import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@Service
public class CommandService {

    private static final Set<String> COMMAND_WORDS = Set.of(
            "submit", "send", "execute", "go",
            "clear chat", "delete last",
            "enable autospeak", "disable autospeak",
            "show help"
    );

    private static final Map<String, Runnable> COMMAND_ACTIONS = new HashMap<>();

    public CommandService() {
        initializeCommandActions();
    }

    private void initializeCommandActions() {
        COMMAND_ACTIONS.put("clear chat", this::clearChat);
        COMMAND_ACTIONS.put("delete last", this::deleteLastMessage);
        COMMAND_ACTIONS.put("enable autospeak", this::enableAutoSpeak);
        COMMAND_ACTIONS.put("disable autospeak", this::disableAutoSpeak);
        COMMAND_ACTIONS.put("show help", this::showHelp);
        COMMAND_ACTIONS.put("submit", () -> executeSubmit("default")); // Submit with optional parameters
        COMMAND_ACTIONS.put("send", () -> executeSubmit("default"));
        COMMAND_ACTIONS.put("execute", () -> executeSubmit("default"));
        COMMAND_ACTIONS.put("go", () -> executeSubmit("default"));
    }

    public Optional<String> detectCommand(String transcript) {
        String cleanedTranscript = transcript.trim().toLowerCase();
        return COMMAND_WORDS.stream()
                .filter(cleanedTranscript::contains)
                .findFirst();
    }

    public String executeCommand(String command) {
        Runnable action = COMMAND_ACTIONS.get(command);
        if (action != null) {
            action.run();
            return command.toUpperCase().replace(" ", "_"); // Example: "clear chat" → "CLEAR_CHAT"
        }
        return "UNKNOWN_COMMAND";
    }

    // ** Actual Actions **

    private void clearChat() {
        System.out.println("🧹 Chat has been cleared!");
        // Backend logic: Notify frontend, update logs, reset storage, etc.
    }

    private void deleteLastMessage() {
        System.out.println("❌ Last message deleted!");
        // Backend logic to remove the last stored message
    }

    private void enableAutoSpeak() {
        System.out.println("🔊 Auto-speak enabled!");
        // Logic to store this state
    }

    private void disableAutoSpeak() {
        System.out.println("🔇 Auto-speak disabled!");
        // Logic to store this state
    }

    private void showHelp() {
        System.out.println("📜 Showing help info...");
        // Potentially return a list of available commands to the frontend
    }

    private void executeSubmit(String parameter) {
        System.out.println("🚀 Submitting with param: " + parameter);
        // Execute actual submission logic here
    }
}
