package com.apisentinel.monitor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MonitorDtos {
    private MonitorDtos() {
    }

    public record MonitorRequest(
            @NotBlank String name,
            @NotBlank String url,
            @NotNull MonitorHttpMethod method,
            @Min(100) @Max(599) Integer expectedStatusCode,
            @Min(1) @Max(30) Integer timeoutSeconds,
            @Min(60) Integer intervalSeconds,
            @Min(1) Integer latencyThresholdMs,
            @Min(1) Integer failureThreshold,
            Map<String, String> headers,
            String body,
            Boolean enabled
    ) {
    }

    public record MonitorResponse(
            UUID id,
            UUID projectId,
            String name,
            String url,
            MonitorHttpMethod method,
            int expectedStatusCode,
            int timeoutSeconds,
            int intervalSeconds,
            int latencyThresholdMs,
            int failureThreshold,
            int consecutiveFailures,
            boolean enabled,
            MonitorState state,
            Map<String, String> headers,
            String body,
            Instant lastCheckedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CheckResponse(
            UUID id,
            UUID monitorId,
            Instant checkedAt,
            Integer latencyMs,
            Integer statusCode,
            boolean success,
            String errorCategory,
            String responseSnippet
    ) {
    }

    public record MetricsResponse(
            UUID monitorId,
            String range,
            double uptimePercentage,
            double averageLatencyMs,
            double p95LatencyMs,
            long failureCount,
            long incidentCount,
            List<CheckResponse> recentChecks
    ) {
    }
}
