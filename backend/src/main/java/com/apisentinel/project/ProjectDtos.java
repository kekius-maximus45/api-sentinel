package com.apisentinel.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotNull UUID organizationId,
            @NotBlank String name
    ) {
    }

    public record ProjectResponse(
            UUID id,
            UUID organizationId,
            String name,
            String slug,
            boolean publicStatusEnabled,
            Instant createdAt
    ) {
    }
}
