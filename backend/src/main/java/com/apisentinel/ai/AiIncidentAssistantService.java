package com.apisentinel.ai;

import com.apisentinel.alert.AlertCondition;
import com.apisentinel.alert.AlertDtos;
import com.apisentinel.alert.AlertService;
import com.apisentinel.auth.CurrentUserService;
import com.apisentinel.check.MonitorCheckRepository;
import com.apisentinel.common.BadRequestException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.config.AppProperties;
import com.apisentinel.incident.IncidentRepository;
import com.apisentinel.monitor.MetricsCalculator;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.organization.Organization;
import com.apisentinel.organization.UserAccount;
import com.apisentinel.organization.UserRepository;
import com.apisentinel.project.Project;
import com.apisentinel.project.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiIncidentAssistantService {
    private static final String CREATE_ALERT_ACTION = "create-latency-alert-rule";

    private final AccessControlService accessControlService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AiAuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;
    private final AlertService alertService;
    private final AppProperties properties;
    private final WebClient.Builder webClientBuilder;

    public AiIncidentAssistantService(
            AccessControlService accessControlService,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            AiAuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            MonitorRepository monitorRepository,
            MonitorCheckRepository checkRepository,
            IncidentRepository incidentRepository,
            AlertService alertService,
            AppProperties properties,
            WebClient.Builder webClientBuilder
    ) {
        this.accessControlService = accessControlService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.monitorRepository = monitorRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
        this.alertService = alertService;
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
    }

    @Transactional
    public AiDtos.ChatResponse chat(AiDtos.ChatRequest request) {
        Organization organization = accessControlService.requireOrganization(request.organizationId());
        UserAccount user = userRepository.findById(currentUserService.currentUser().id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Project project = resolveProject(request.projectId(), organization.getId());
        Monitor monitor = resolveMonitor(request.monitorId(), project);
        List<String> tools = new ArrayList<>();

        if (isCreateAlertIntent(request.message())) {
            if (!CREATE_ALERT_ACTION.equals(request.confirmedActionId())) {
                audit(user, organization, request.message(), "create_alert_rule[pending_confirmation]", "CONFIRMATION_REQUIRED");
                return new AiDtos.ChatResponse(
                        "I can create a latency alert rule for this monitor. Confirm the action before I change alert configuration.",
                        List.of("create_alert_rule"),
                        citations(project, monitor),
                        true,
                        CREATE_ALERT_ACTION
                );
            }
            alertService.createRule(project.getId(), new AlertDtos.AlertRuleRequest(
                    monitor == null ? null : monitor.getId(),
                    request.notificationChannelId(),
                    "AI suggested latency alert",
                    AlertCondition.LATENCY_THRESHOLD_EXCEEDED,
                    monitor == null ? 1000 : monitor.getLatencyThresholdMs(),
                    true
            ));
            audit(user, organization, request.message(), "create_alert_rule", "SUCCESS");
            return new AiDtos.ChatResponse(
                    "Created the latency alert rule after explicit confirmation.",
                    List.of("create_alert_rule"),
                    citations(project, monitor),
                    false,
                    null
            );
        }

        String response = buildLocalAnswer(project, monitor, request.message(), tools);
        if (shouldUseOpenAi()) {
            response = callOpenAi(project, monitor, request.message(), response);
        }
        audit(user, organization, request.message(), String.join(",", tools), "SUCCESS");
        return new AiDtos.ChatResponse(response, tools, citations(project, monitor), false, null);
    }

    @Transactional(readOnly = true)
    public List<AiDtos.AuditLogResponse> auditLogs(UUID organizationId) {
        accessControlService.requireOrganization(organizationId);
        return auditLogRepository.findTop100ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(log -> new AiDtos.AuditLogResponse(
                        log.getId(),
                        log.getUser() == null ? null : log.getUser().getId(),
                        log.getOrganization().getId(),
                        log.getPromptSummary(),
                        log.getToolsInvoked(),
                        log.getResultStatus(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private String buildLocalAnswer(Project project, Monitor monitor, String message, List<String> tools) {
        if (monitor != null) {
            tools.add("get_monitor_health");
            tools.add("get_latency_stats");
            var since = Instant.now().minus(Duration.ofHours(24));
            var checks = checkRepository.findAllByMonitorIdAndCheckedAtBetweenOrderByCheckedAtAsc(monitor.getId(), since, Instant.now());
            long failures = checks.stream().filter(check -> !check.isSuccess()).count();
            return "Monitor " + monitor.getName() + " is currently " + monitor.getState()
                    + ". Last 24h uptime is " + round(MetricsCalculator.uptimePercentage(checks)) + "%, average latency is "
                    + round(MetricsCalculator.averageLatency(checks)) + " ms, p95 latency is "
                    + round(MetricsCalculator.p95Latency(checks)) + " ms, with " + failures
                    + " failed checks. " + customerFacingDraft(monitor.getState().name(), monitor.getName());
        }
        tools.add("summarize_recent_incidents");
        long incidents = incidentRepository.findAllByProjectIdOrderByStartedAtDesc(project.getId()).stream().limit(10).count();
        return "Project " + project.getName() + " has " + incidents + " recent incidents in the current incident list. "
                + "Ask about a specific monitor to include latency and check-history details.";
    }

    private String callOpenAi(Project project, Monitor monitor, String userMessage, String context) {
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.ai().openai().model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an API incident assistant. Use the supplied API Sentinel context and keep responses operational."),
                            Map.of("role", "user", "content", userMessage + "\n\nContext:\n" + context + "\nProject: " + project.getName() + "\nMonitor: " + (monitor == null ? "none" : monitor.getName()))
                    )
            );
            JsonNode json = webClientBuilder.build()
                    .post()
                    .uri(properties.ai().openai().baseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.ai().openai().apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(20));
            String content = json == null ? "" : json.at("/choices/0/message/content").asText();
            return content.isBlank() ? context : content;
        } catch (RuntimeException exception) {
            return context + " The configured LLM provider did not respond, so this response used local incident data only.";
        }
    }

    private Project resolveProject(UUID projectId, UUID organizationId) {
        if (projectId == null) {
            return projectRepository.findAllByOrganizationId(organizationId).stream()
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Create a project before using the assistant"));
        }
        Project project = accessControlService.requireProject(projectId);
        if (!project.getOrganization().getId().equals(organizationId)) {
            throw new BadRequestException("projectId must belong to organizationId");
        }
        return project;
    }

    private Monitor resolveMonitor(UUID monitorId, Project project) {
        if (monitorId == null) {
            return null;
        }
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor not found"));
        if (!monitor.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("monitorId must belong to projectId");
        }
        return monitor;
    }

    private boolean isCreateAlertIntent(String message) {
        // Previous implementation used simple .contains("create") && .contains("alert")
        // which would match "I don't want to create an alert". This regex requires
        // the words to appear in order without negations between them.
        String normalized = message.toLowerCase();
        return normalized.matches(".*\\bcreate\\b.*\\balert\\b.*")
                && !normalized.matches(".*(don't|dont|do not|not|never|stop|remove|delete)\\s+(create|add|make).*");
    }

    private boolean shouldUseOpenAi() {
        return properties.ai() != null
                && "openai".equalsIgnoreCase(properties.ai().provider())
                && properties.ai().openai() != null
                && properties.ai().openai().apiKey() != null
                && !properties.ai().openai().apiKey().isBlank();
    }

    private void audit(UserAccount user, Organization organization, String prompt, String toolsInvoked, String resultStatus) {
        String summary = prompt.length() > 500 ? prompt.substring(0, 500) : prompt;
        auditLogRepository.save(new AiAuditLog(user, organization, summary, toolsInvoked, resultStatus));
    }

    private List<String> citations(Project project, Monitor monitor) {
        List<String> citations = new ArrayList<>();
        citations.add("project:" + project.getId());
        if (monitor != null) {
            citations.add("monitor:" + monitor.getId());
        }
        return citations;
    }

    private String customerFacingDraft(String state, String monitorName) {
        return switch (state) {
            case "DOWN" -> "Draft status update: We are investigating elevated errors for " + monitorName + " and will post the next update after mitigation is confirmed.";
            case "DEGRADED" -> "Draft status update: " + monitorName + " is responding slower than usual; core availability remains under observation.";
            default -> "Draft status update: " + monitorName + " is currently operating normally.";
        };
    }

    private double round(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }
}
