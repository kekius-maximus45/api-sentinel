package com.apisentinel.alert;

import com.apisentinel.common.BadRequestException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.incident.Incident;
import com.apisentinel.incident.IncidentEvent;
import com.apisentinel.incident.IncidentEventRepository;
import com.apisentinel.incident.IncidentEventType;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.project.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlertService {
    private final AlertRuleRepository alertRuleRepository;
    private final NotificationChannelRepository channelRepository;
    private final MonitorRepository monitorRepository;
    private final AccessControlService accessControlService;
    private final WebhookPayloadFactory payloadFactory;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final IncidentEventRepository incidentEventRepository;

    public AlertService(
            AlertRuleRepository alertRuleRepository,
            NotificationChannelRepository channelRepository,
            MonitorRepository monitorRepository,
            AccessControlService accessControlService,
            WebhookPayloadFactory payloadFactory,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            IncidentEventRepository incidentEventRepository
    ) {
        this.alertRuleRepository = alertRuleRepository;
        this.channelRepository = channelRepository;
        this.monitorRepository = monitorRepository;
        this.accessControlService = accessControlService;
        this.payloadFactory = payloadFactory;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.incidentEventRepository = incidentEventRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertDtos.AlertRuleResponse> listRules(UUID projectId) {
        accessControlService.requireProject(projectId);
        return alertRuleRepository.findAllByProjectId(projectId).stream().map(this::toRuleResponse).toList();
    }

    @Transactional
    public AlertDtos.AlertRuleResponse createRule(UUID projectId, AlertDtos.AlertRuleRequest request) {
        Project project = accessControlService.requireProject(projectId);
        AlertRule rule = createRuleInternal(project, request);
        return toRuleResponse(rule);
    }

    @Transactional
    public AlertRule createRuleInternal(Project project, AlertDtos.AlertRuleRequest request) {
        Monitor monitor = resolveMonitor(project, request.monitorId());
        NotificationChannel channel = resolveChannel(project, request.notificationChannelId());
        AlertRule rule = alertRuleRepository.save(new AlertRule(
                project,
                monitor,
                channel,
                request.name().trim(),
                request.condition(),
                request.thresholdMs(),
                request.enabled() == null || request.enabled()
        ));
        return rule;
    }

    @Transactional
    public AlertDtos.AlertRuleResponse updateRule(UUID ruleId, AlertDtos.AlertRuleRequest request) {
        AlertRule rule = requireRule(ruleId);
        Project project = rule.getProject();
        rule.update(
                resolveMonitor(project, request.monitorId()),
                resolveChannel(project, request.notificationChannelId()),
                request.name().trim(),
                request.condition(),
                request.thresholdMs(),
                request.enabled() == null || request.enabled()
        );
        return toRuleResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        AlertRule rule = requireRule(ruleId);
        alertRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<AlertDtos.ChannelResponse> listChannels(UUID projectId) {
        accessControlService.requireProject(projectId);
        return channelRepository.findAllByProjectId(projectId).stream().map(this::toChannelResponse).toList();
    }

    @Transactional
    public AlertDtos.ChannelResponse createChannel(UUID projectId, AlertDtos.ChannelRequest request) {
        Project project = accessControlService.requireProject(projectId);
        validateWebhookUrl(request.webhookUrl());
        NotificationChannel channel = channelRepository.save(new NotificationChannel(
                project,
                request.name().trim(),
                NotificationChannelType.WEBHOOK,
                request.webhookUrl().trim()
        ));
        return toChannelResponse(channel);
    }

    @Transactional(readOnly = true)
    public void testChannel(UUID channelId) {
        NotificationChannel channel = requireChannel(channelId);
        Map<String, Object> payload = Map.of(
                "event", "TEST",
                "channel", channel.getName(),
                "message", "API Sentinel webhook test"
        );
        sendWebhook(channel, payload);
    }

    public void dispatch(Monitor monitor, Incident incident, AlertCondition condition, Integer latencyMs) {
        List<AlertRule> rules = alertRuleRepository.findAllByProjectIdAndEnabledTrue(monitor.getProject().getId());
        for (AlertRule rule : rules) {
            if (rule.getCondition() != condition || !matchesMonitor(rule, monitor) || !matchesThreshold(rule, condition, latencyMs)) {
                continue;
            }
            NotificationChannel channel = rule.getNotificationChannel();
            if (channel == null) {
                continue;
            }
            Map<String, Object> payload = payloadFactory.build(monitor, incident, condition, latencyMs);
            try {
                sendWebhook(channel, payload);
                addAlertEvent(incident, "Webhook delivered to " + channel.getName(), Map.of("ruleId", rule.getId(), "channelId", channel.getId(), "status", "SENT"));
            } catch (RuntimeException exception) {
                addAlertEvent(incident, "Webhook failed for " + channel.getName(), Map.of("ruleId", rule.getId(), "channelId", channel.getId(), "status", "FAILED", "error", exception.getMessage()));
            }
        }
    }

    private boolean matchesMonitor(AlertRule rule, Monitor monitor) {
        return rule.getMonitor() == null || rule.getMonitor().getId().equals(monitor.getId());
    }

    private boolean matchesThreshold(AlertRule rule, AlertCondition condition, Integer latencyMs) {
        if (condition != AlertCondition.LATENCY_THRESHOLD_EXCEEDED) {
            return true;
        }
        if (latencyMs == null) {
            return false;
        }
        return rule.getThresholdMs() == null || latencyMs >= rule.getThresholdMs();
    }

    private void sendWebhook(NotificationChannel channel, Map<String, Object> payload) {
        webClientBuilder.build()
                .post()
                .uri(channel.getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));
    }

    private void addAlertEvent(Incident incident, String message, Map<String, Object> metadata) {
        if (incident == null) {
            return;
        }
        try {
            incidentEventRepository.save(new IncidentEvent(
                    incident,
                    IncidentEventType.ALERT_SENT,
                    message,
                    objectMapper.writeValueAsString(metadata),
                    null
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to record alert event", exception);
        }
    }

    private AlertRule requireRule(UUID ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Alert rule not found"));
        accessControlService.requireProject(rule.getProject().getId());
        return rule;
    }

    private NotificationChannel requireChannel(UUID channelId) {
        NotificationChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Notification channel not found"));
        accessControlService.requireProject(channel.getProject().getId());
        return channel;
    }

    private Monitor resolveMonitor(Project project, UUID monitorId) {
        if (monitorId == null) {
            return null;
        }
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor not found"));
        if (!monitor.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("Monitor must belong to the project");
        }
        return monitor;
    }

    private NotificationChannel resolveChannel(Project project, UUID channelId) {
        if (channelId == null) {
            return null;
        }
        NotificationChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Notification channel not found"));
        if (!channel.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("Notification channel must belong to the project");
        }
        return channel;
    }

    private void validateWebhookUrl(String webhookUrl) {
        try {
            URI uri = URI.create(webhookUrl);
            if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                throw new BadRequestException("webhookUrl must be HTTP or HTTPS");
            }
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("webhookUrl must be valid");
        }
    }

    private AlertDtos.AlertRuleResponse toRuleResponse(AlertRule rule) {
        return new AlertDtos.AlertRuleResponse(
                rule.getId(),
                rule.getProject().getId(),
                rule.getMonitor() == null ? null : rule.getMonitor().getId(),
                rule.getNotificationChannel() == null ? null : rule.getNotificationChannel().getId(),
                rule.getName(),
                rule.getCondition(),
                rule.getThresholdMs(),
                rule.isEnabled(),
                rule.getCreatedAt()
        );
    }

    private AlertDtos.ChannelResponse toChannelResponse(NotificationChannel channel) {
        return new AlertDtos.ChannelResponse(
                channel.getId(),
                channel.getProject().getId(),
                channel.getName(),
                channel.getType(),
                channel.getWebhookUrl(),
                channel.getCreatedAt()
        );
    }
}
