package com.apisentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "api-sentinel")
public record AppProperties(
        String appUrl,
        Cors cors,
        Jwt jwt,
        Ai ai
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long accessTokenMinutes) {
    }

    public record Ai(String provider, OpenAi openai) {
    }

    public record OpenAi(String baseUrl, String apiKey, String model) {
    }
}
