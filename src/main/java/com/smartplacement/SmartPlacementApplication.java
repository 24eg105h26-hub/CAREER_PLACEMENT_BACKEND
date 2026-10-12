package com.smartplacement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class SmartPlacementApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(SmartPlacementApplication.class, args);
    }

    private static void loadDotEnv() {
        for (String candidate : List.of(".env", "../.env", "backend/.env")) {
            Path p = Path.of(candidate);
            if (Files.exists(p)) {
                try {
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                        int idx = trimmed.indexOf('=');
                        if (idx > 0) {
                            String key = trimmed.substring(0, idx).trim();
                            String val = trimmed.substring(idx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    break;
                } catch (Exception ignored) {
                }
            }
        }

        // Support GEMINI_API_KEY alias for AI_API_KEY
        String geminiKey = System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY"));
        String aiKey = System.getProperty("AI_API_KEY", System.getenv("AI_API_KEY"));
        if ((aiKey == null || aiKey.isBlank() || aiKey.startsWith("your-")) && geminiKey != null && !geminiKey.isBlank()) {
            System.setProperty("AI_API_KEY", geminiKey);
        }
    }
}
