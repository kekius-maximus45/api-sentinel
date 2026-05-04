package com.apisentinel.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiDtos {
    private AiDtos() {
    }

    public record ChatRequest(
            @NotNull UUID organizationId,
            UUID projectId,
            UUID monitorId,
            UUID notificationChannelId,
            @NotBlank String message,
            String confirmedActionId
    ) {
    }

    public record ChatResponse(
            String message,
            List<String> toolsInvoked,
            List<String> citations,
            boolean confirmationRequired,
            String actionId
    ) {
    }

    public record AuditLogResponse(
            UUID id,
            UUID userId,
            UUID organizationId,
            String promptSummary,
            String toolsInvoked,
            String resultStatus,
            Instant createdAt
    ) {
    }
}
