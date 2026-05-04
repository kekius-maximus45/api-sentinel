package com.apisentinel.incident;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IncidentDtos {
    private IncidentDtos() {
    }

    public record IncidentResponse(
            UUID id,
            UUID projectId,
            UUID monitorId,
            String monitorName,
            String title,
            IncidentStatus status,
            Instant startedAt,
            Instant resolvedAt,
            List<EventResponse> events
    ) {
    }

    public record EventResponse(
            UUID id,
            IncidentEventType type,
            String message,
            String metadataJson,
            Instant createdAt
    ) {
    }

    public record NoteRequest(@NotBlank String message) {
    }
}
