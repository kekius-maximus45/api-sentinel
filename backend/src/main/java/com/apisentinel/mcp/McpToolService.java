package com.apisentinel.mcp;

import com.apisentinel.alert.AlertCondition;
import com.apisentinel.alert.AlertDtos;
import com.apisentinel.alert.AlertService;
import com.apisentinel.apikey.ApiKey;
import com.apisentinel.apikey.ApiKeyService;
import com.apisentinel.check.MonitorCheck;
import com.apisentinel.check.MonitorCheckRepository;
import com.apisentinel.common.BadRequestException;
import com.apisentinel.common.ForbiddenException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.incident.IncidentRepository;
import com.apisentinel.monitor.MetricsCalculator;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import com.apisentinel.project.Project;
import com.apisentinel.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class McpToolService {
    private final ApiKeyService apiKeyService;
    private final ProjectRepository projectRepository;
    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;
    private final AlertService alertService;

    public McpToolService(
            ApiKeyService apiKeyService,
            ProjectRepository projectRepository,
            MonitorRepository monitorRepository,
            MonitorCheckRepository checkRepository,
            IncidentRepository incidentRepository,
            AlertService alertService
    ) {
        this.apiKeyService = apiKeyService;
        this.projectRepository = projectRepository;
        this.monitorRepository = monitorRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
        this.alertService = alertService;
    }

    @Transactional
    public Map<String, Object> monitorHealth(String apiKey, UUID monitorId) {
        Monitor monitor = authorizeMonitor(apiKey, monitorId);
        MonitorCheck latest = checkRepository.findTopByMonitorIdOrderByCheckedAtDesc(monitorId).orElse(null);
        return Map.of(
                "monitorId", monitor.getId(),
                "name", monitor.getName(),
                "state", monitor.getState().name(),
                "lastCheckedAt", latest == null ? "" : latest.getCheckedAt().toString(),
                "lastLatencyMs", latest == null || latest.getLatencyMs() == null ? "" : latest.getLatencyMs(),
                "lastSuccess", latest != null && latest.isSuccess()
        );
    }

    @Transactional
    public List<Map<String, Object>> incidentHistory(String apiKey, UUID projectId) {
        Project project = authorizeProject(apiKey, projectId);
        return incidentRepository.findAllByProjectIdOrderByStartedAtDesc(project.getId()).stream()
                .limit(20)
                .map(incident -> Map.<String, Object>of(
                        "incidentId", incident.getId(),
                        "monitor", incident.getMonitor().getName(),
                        "title", incident.getTitle(),
                        "status", incident.getStatus().name(),
                        "startedAt", incident.getStartedAt().toString(),
                        "resolvedAt", incident.getResolvedAt() == null ? "" : incident.getResolvedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> latencyStats(String apiKey, UUID monitorId, String range) {
        Monitor monitor = authorizeMonitor(apiKey, monitorId);
        Duration duration = switch (range == null ? "24h" : range) {
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            case "24h" -> Duration.ofHours(24);
            default -> throw new BadRequestException("range must be 24h, 7d, or 30d");
        };
        List<MonitorCheck> checks = checkRepository.findAllByMonitorIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                monitor.getId(),
                Instant.now().minus(duration),
                Instant.now()
        );
        return Map.of(
                "monitorId", monitor.getId(),
                "range", range == null ? "24h" : range,
                "uptimePercentage", MetricsCalculator.uptimePercentage(checks),
                "averageLatencyMs", MetricsCalculator.averageLatency(checks),
                "p95LatencyMs", MetricsCalculator.p95Latency(checks),
                "failureCount", checks.stream().filter(check -> !check.isSuccess()).count()
        );
    }

    @Transactional
    public String summarizeRecentIncidents(String apiKey, UUID projectId) {
        Project project = authorizeProject(apiKey, projectId);
        var incidents = incidentRepository.findAllByProjectIdOrderByStartedAtDesc(project.getId()).stream().limit(5).toList();
        if (incidents.isEmpty()) {
            return "No recent incidents for " + project.getName() + ".";
        }
        return "Recent incidents for " + project.getName() + ": " + incidents.stream()
                .map(incident -> incident.getMonitor().getName() + " " + incident.getStatus() + " since " + incident.getStartedAt())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    @Transactional
    public String draftStatusUpdate(String apiKey, UUID monitorId) {
        Monitor monitor = authorizeMonitor(apiKey, monitorId);
        return switch (monitor.getState()) {
            case DOWN -> "We are investigating an outage affecting " + monitor.getName() + ". We will share another update after mitigation is underway.";
            case DEGRADED -> monitor.getName() + " is currently degraded. Requests may be slower than normal while we investigate.";
            case PAUSED -> monitor.getName() + " is paused and is not currently included in automated monitoring.";
            case UP -> monitor.getName() + " is operating normally.";
        };
    }

    @Transactional
    public Map<String, Object> createAlertRule(String apiKey, UUID projectId, UUID monitorId, UUID notificationChannelId,
                                               String name, String condition, Integer thresholdMs, boolean confirmed) {
        Project project = authorizeProject(apiKey, projectId);
        if (!confirmed) {
            return Map.of(
                    "confirmationRequired", true,
                    "message", "create_alert_rule requires confirmed=true"
            );
        }
        AlertCondition alertCondition = AlertCondition.valueOf(condition);
        var rule = alertService.createRuleInternal(project, new AlertDtos.AlertRuleRequest(
                monitorId,
                notificationChannelId,
                name,
                alertCondition,
                thresholdMs,
                true
        ));
        return Map.of(
                "confirmationRequired", false,
                "ruleId", rule.getId(),
                "name", rule.getName(),
                "condition", rule.getCondition().name()
        );
    }

    private Project authorizeProject(String rawApiKey, UUID projectId) {
        ApiKey apiKey = apiKeyService.authenticate(rawApiKey);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (!project.getOrganization().getId().equals(apiKey.getOrganization().getId())) {
            throw new ForbiddenException("API key is not scoped to this project");
        }
        return project;
    }

    private Monitor authorizeMonitor(String rawApiKey, UUID monitorId) {
        ApiKey apiKey = apiKeyService.authenticate(rawApiKey);
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor not found"));
        if (!monitor.getProject().getOrganization().getId().equals(apiKey.getOrganization().getId())) {
            throw new ForbiddenException("API key is not scoped to this monitor");
        }
        return monitor;
    }
}
