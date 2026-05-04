package com.apisentinel.statuspage;

import com.apisentinel.check.MonitorCheckRepository;
import com.apisentinel.incident.Incident;
import com.apisentinel.incident.IncidentRepository;
import com.apisentinel.incident.IncidentStatus;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import com.apisentinel.monitor.MonitorState;
import com.apisentinel.project.Project;
import com.apisentinel.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class StatusPageService {
    private final ProjectService projectService;
    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;

    public StatusPageService(
            ProjectService projectService,
            MonitorRepository monitorRepository,
            MonitorCheckRepository checkRepository,
            IncidentRepository incidentRepository
    ) {
        this.projectService = projectService;
        this.monitorRepository = monitorRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public StatusPageDtos.StatusPageResponse get(String slug) {
        Project project = projectService.requirePublicProject(slug);
        List<Monitor> monitors = monitorRepository.findAllByProjectId(project.getId());
        List<StatusPageDtos.PublicMonitorStatus> publicMonitors = monitors.stream()
                .sorted(Comparator.comparing(Monitor::getName))
                .map(this::toPublicMonitor)
                .toList();
        MonitorState overall = calculateOverall(publicMonitors);
        List<StatusPageDtos.PublicIncident> active = incidentRepository
                .findAllByProjectIdAndStatusOrderByStartedAtDesc(project.getId(), IncidentStatus.OPEN)
                .stream()
                .map(this::toPublicIncident)
                .toList();
        List<StatusPageDtos.PublicIncident> resolved = incidentRepository
                .findAllByProjectIdAndStatusAndResolvedAtAfterOrderByResolvedAtDesc(project.getId(), IncidentStatus.RESOLVED, Instant.now().minusSeconds(30L * 24L * 60L * 60L))
                .stream()
                .map(this::toPublicIncident)
                .toList();
        return new StatusPageDtos.StatusPageResponse(
                project.getId(),
                project.getName(),
                project.getSlug(),
                overall,
                publicMonitors,
                active,
                resolved
        );
    }

    public String renderHtml(StatusPageDtos.StatusPageResponse status) {
        String rows = status.monitors().stream()
                .map(monitor -> "<li><strong>" + escape(monitor.name()) + "</strong><span>" + monitor.state() + "</span></li>")
                .reduce("", String::concat);
        String incidents = status.activeIncidents().isEmpty()
                ? "<p>No active incidents.</p>"
                : status.activeIncidents().stream()
                .map(incident -> "<p>" + escape(incident.title()) + "</p>")
                .reduce("", String::concat);
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s status</title>
                  <style>
                    body{font-family:Inter,Arial,sans-serif;margin:0;background:#f7f9fb;color:#172026}
                    main{max-width:860px;margin:0 auto;padding:48px 20px}
                    header{display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #d8dee6;padding-bottom:20px}
                    .status{font-weight:700;color:#0f766e}
                    ul{list-style:none;padding:0;margin:28px 0}
                    li{display:flex;justify-content:space-between;padding:14px 0;border-bottom:1px solid #d8dee6}
                  </style>
                </head>
                <body><main><header><h1>%s</h1><div class="status">%s</div></header><ul>%s</ul><section><h2>Active incidents</h2>%s</section></main></body>
                </html>
                """.formatted(
                escape(status.projectName()),
                escape(status.projectName()),
                status.overallStatus(),
                rows,
                incidents
        );
    }

    private StatusPageDtos.PublicMonitorStatus toPublicMonitor(Monitor monitor) {
        var latest = checkRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitor.getId());
        return new StatusPageDtos.PublicMonitorStatus(
                monitor.getId(),
                monitor.getName(),
                monitor.getState(),
                latest.map(check -> check.getLatencyMs()).orElse(null),
                latest.map(check -> check.getCheckedAt()).orElse(null)
        );
    }

    private StatusPageDtos.PublicIncident toPublicIncident(Incident incident) {
        return new StatusPageDtos.PublicIncident(
                incident.getId(),
                incident.getTitle(),
                incident.getMonitor().getName(),
                incident.getStatus(),
                incident.getStartedAt(),
                incident.getResolvedAt()
        );
    }

    private MonitorState calculateOverall(List<StatusPageDtos.PublicMonitorStatus> monitors) {
        if (monitors.stream().anyMatch(monitor -> monitor.state() == MonitorState.DOWN)) {
            return MonitorState.DOWN;
        }
        if (monitors.stream().anyMatch(monitor -> monitor.state() == MonitorState.DEGRADED)) {
            return MonitorState.DEGRADED;
        }
        if (monitors.stream().allMatch(monitor -> monitor.state() == MonitorState.PAUSED)) {
            return MonitorState.PAUSED;
        }
        return MonitorState.UP;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
