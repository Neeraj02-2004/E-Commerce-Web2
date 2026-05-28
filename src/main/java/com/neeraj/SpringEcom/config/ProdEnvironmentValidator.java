package com.neeraj.SpringEcom.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@Profile("prod")
public class ProdEnvironmentValidator implements ApplicationRunner {

    private final Environment environment;

    public ProdEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        requireValue(errors, "DB_URL", "spring.datasource.url");
        requireValue(errors, "DB_USERNAME", "spring.datasource.username");
        requireSecret(errors, "DB_PASSWORD", "spring.datasource.password");

        requireValue(errors, "REDIS_HOST", "spring.data.redis.host");
        requireSecret(errors, "REDIS_PASSWORD", "spring.data.redis.password");

        requireValue(errors, "GOOGLE_CLIENT_ID", "spring.security.oauth2.client.registration.google.client-id");
        requireSecret(errors, "GOOGLE_CLIENT_SECRET", "spring.security.oauth2.client.registration.google.client-secret");

        requireValue(errors, "RAZORPAY_KEY_ID", "razorpay.key-id");
        requireSecret(errors, "RAZORPAY_KEY_SECRET", "razorpay.key-secret");
        requireSecret(errors, "RAZORPAY_WEBHOOK_SECRET", "razorpay.webhook-secret");

        requireValue(errors, "CORS_ORIGINS", "app.cors.origins");

        validateJwtSecret(errors);
        validateCors(errors);
        validateStorage(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid production configuration: " + String.join(" ", errors));
        }
    }

    private void validateJwtSecret(List<String> errors) {
        String jwtSecret = environment.getProperty("app.jwt.secret");

        if (isBlank(jwtSecret) || isPlaceholder(jwtSecret) || jwtSecret.length() < 32) {
            errors.add("JWT_SECRET must be a real Base64-encoded high-entropy secret.");
            return;
        }

        try {
            byte[] decodedSecret = Base64.getDecoder().decode(jwtSecret);

            if (decodedSecret.length < 32) {
                errors.add("JWT_SECRET must decode to at least 32 bytes.");
            }
        } catch (IllegalArgumentException e) {
            errors.add("JWT_SECRET must be valid Base64.");
        }
    }

    private void validateCors(List<String> errors) {
        String corsOrigins = environment.getProperty("app.cors.origins", "");

        if (corsOrigins.contains("localhost") || corsOrigins.contains("127.0.0.1")) {
            errors.add("CORS_ORIGINS must use the real frontend domain in production, not localhost.");
        }
    }

    private void validateStorage(List<String> errors) {
        String storageType = environment.getProperty("app.storage.type", "cloudinary").trim().toLowerCase();

        if (!storageType.equals("local") && !storageType.equals("cloudinary")) {
            errors.add("STORAGE_TYPE must be either local or cloudinary.");
            return;
        }

        if (storageType.equals("cloudinary")) {
            requireValue(errors, "CLOUDINARY_CLOUD_NAME", "cloudinary.cloud-name");
            requireValue(errors, "CLOUDINARY_API_KEY", "cloudinary.api-key");
            requireSecret(errors, "CLOUDINARY_API_SECRET", "cloudinary.api-secret");
        }
    }

    private void requireSecret(List<String> errors, String envName, String propertyName) {
        String value = environment.getProperty(propertyName);

        if (isBlank(value) || isPlaceholder(value)) {
            errors.add(envName + " must be set to a real secret.");
        }
    }

    private void requireValue(List<String> errors, String envName, String propertyName) {
        String value = environment.getProperty(propertyName);

        if (isBlank(value) || isPlaceholder(value)) {
            errors.add(envName + " must be set.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase().trim();
        return normalized.contains("change-me")
                || normalized.contains("your-")
                || normalized.contains("<")
                || normalized.contains(">");
    }
}
