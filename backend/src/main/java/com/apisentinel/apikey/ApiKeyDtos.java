package com.apisentinel.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class ApiKeyDtos {
    private ApiKeyDtos() {
    }

    public record ApiKeyRequest(@NotNull UUID organizationId, @NotBlank String name) {
    }

    public record ApiKeyResponse(
            UUID id,
            UUID organizationId,
            String name,
            String keyPrefix,
            Instant createdAt,
            Instant lastUsedAt
    ) {
    }

    public record ApiKeyCreateResponse(ApiKeyResponse apiKey, String secret) {
    }
}
