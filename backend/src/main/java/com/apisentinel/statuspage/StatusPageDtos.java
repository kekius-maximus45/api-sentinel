package com.apisentinel.statuspage;

import com.apisentinel.incident.IncidentStatus;
import com.apisentinel.monitor.MonitorState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StatusPageDtos {
    private StatusPageDtos() {
    }

    public record StatusPageResponse(
            UUID projectId,
            String projectName,
            String slug,
            MonitorState overallStatus,
            List<PublicMonitorStatus> monitors,
            List<PublicIncident> activeIncidents,
            List<PublicIncident> recentResolvedIncidents
    ) {
    }

    public record PublicMonitorStatus(
            UUID id,
            String name,
            MonitorState state,
            Integer lastLatencyMs,
            Instant lastCheckedAt
    ) {
    }

    public record PublicIncident(
            UUID id,
            String title,
            String monitorName,
            IncidentStatus status,
            Instant startedAt,
            Instant resolvedAt
    ) {
    }
}
