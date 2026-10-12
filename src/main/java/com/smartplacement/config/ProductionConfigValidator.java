package com.smartplacement.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
public class ProductionConfigValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${app.cors.allowed-origins:}")
    private String corsOrigins;

    @PostConstruct
    public void validateProductionConfig() {
        log.info("Validating production deployment configuration...");

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32 || jwtSecret.contains("placeholder") || jwtSecret.equals("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")) {
            throw new IllegalStateException("FATAL: JWT_SECRET environment variable is missing, insecure, or using the default dev key in production! Set a secure JWT_SECRET of at least 32 characters.");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("FATAL: DB_URL or MYSQL_URL environment variable must be explicitly defined in production!");
        }

        if (corsOrigins != null && (corsOrigins.contains("*") || corsOrigins.isBlank())) {
            throw new IllegalStateException("FATAL: CORS_ALLOWED_ORIGINS cannot be empty or wildcard '*' in production! Specify exact frontend domain(s).");
        }

        log.info("Production configuration validation passed successfully.");
    }
}
