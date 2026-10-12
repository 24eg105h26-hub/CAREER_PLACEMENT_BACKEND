package com.smartplacement.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("backend/.env");
        }
        if (!envFile.exists()) {
            envFile = new File("../.env");
        }

        if (envFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(envFile.toPath());
                Map<String, Object> props = new HashMap<>();
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    props.put(key, value);
                }
                if (!props.isEmpty()) {
                    environment.getPropertySources().addLast(new MapPropertySource("dotenvFile", props));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
