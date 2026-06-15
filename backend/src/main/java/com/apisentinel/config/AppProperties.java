package com.apisentinel.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "api-sentinel")
public record AppProperties(
        String appUrl,
        Cors cors,
        Jwt jwt,
        Ai ai
) {
    // Reject startup if the default dev secret is used outside of local/test profiles.
    // Set JWT_SECRET env var to at least 32 random characters in production.
    @PostConstruct
    void validate() {
        if (jwt != null && jwt.secret() != null && jwt.secret().contains("change-me")) {
            String activeProfiles = System.getProperty("spring.profiles.active", "");
            if (!activeProfiles.contains("local") && !activeProfiles.contains("test")) {
                throw new IllegalStateException(
                        "JWT_SECRET must be set to a secure value in production. " +
                        "Generate one with: openssl rand -base64 48"
                );
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long accessTokenMinutes) {
    }

    public record Ai(String provider, OpenAi openai) {
    }

    public record OpenAi(String baseUrl, String apiKey, String model) {
    }
}
