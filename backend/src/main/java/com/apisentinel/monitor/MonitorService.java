package com.apisentinel.monitor;

import com.apisentinel.check.MonitorCheck;
import com.apisentinel.check.MonitorCheckRepository;
import com.apisentinel.common.BadRequestException;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.incident.IncidentRepository;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.project.Project;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MonitorService {
    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;
    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    public MonitorService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository checkRepository,
            IncidentRepository incidentRepository,
            AccessControlService accessControlService,
            ObjectMapper objectMapper
    ) {
        this.monitorRepository = monitorRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
        this.accessControlService = accessControlService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<MonitorDtos.MonitorResponse> list(UUID projectId) {
        accessControlService.requireProject(projectId);
        return monitorRepository.findAllByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MonitorDtos.MonitorResponse create(UUID projectId, MonitorDtos.MonitorRequest request) {
        Project project = accessControlService.requireProject(projectId);
        ValidatedMonitorInput input = validate(request);
        Monitor monitor = monitorRepository.save(new Monitor(
                project,
                input.name(),
                input.url(),
                input.method(),
                input.expectedStatusCode(),
                input.timeoutSeconds(),
                input.intervalSeconds(),
                input.latencyThresholdMs(),
                input.failureThreshold(),
                input.headersJson(),
                input.body()
        ));
        if (!input.enabled()) {
            monitor.markPaused();
        }
        return toResponse(monitor);
    }

    @Transactional(readOnly = true)
    public MonitorDtos.MonitorResponse get(UUID monitorId) {
        return toResponse(requireMonitor(monitorId));
    }

    @Transactional
    public MonitorDtos.MonitorResponse update(UUID monitorId, MonitorDtos.MonitorRequest request) {
        Monitor monitor = requireMonitor(monitorId);
        ValidatedMonitorInput input = validate(request);
        monitor.updateConfiguration(
                input.name(),
                input.url(),
                input.method(),
                input.expectedStatusCode(),
                input.timeoutSeconds(),
                input.intervalSeconds(),
                input.latencyThresholdMs(),
                input.failureThreshold(),
                input.headersJson(),
                input.body(),
                input.enabled()
        );
        return toResponse(monitor);
    }

    @Transactional
    public void delete(UUID monitorId) {
        Monitor monitor = requireMonitor(monitorId);
        monitorRepository.delete(monitor);
    }

    @Transactional
    public MonitorDtos.MonitorResponse pause(UUID monitorId) {
        Monitor monitor = requireMonitor(monitorId);
        monitor.markPaused();
        return toResponse(monitor);
    }

    @Transactional
    public MonitorDtos.MonitorResponse resume(UUID monitorId) {
        Monitor monitor = requireMonitor(monitorId);
        monitor.markEnabled();
        return toResponse(monitor);
    }

    @Transactional(readOnly = true)
    public List<MonitorDtos.CheckResponse> checks(UUID monitorId) {
        requireMonitor(monitorId);
        return checkRepository.findTop20ByMonitorIdOrderByCheckedAtDesc(monitorId).stream()
                .map(this::toCheckResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitorDtos.MetricsResponse metrics(UUID monitorId, String range) {
        requireMonitor(monitorId);
        Duration duration = switch (range == null ? "24h" : range) {
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            case "24h" -> Duration.ofHours(24);
            default -> throw new BadRequestException("range must be one of 24h, 7d, 30d");
        };
        Instant to = Instant.now();
        Instant from = to.minus(duration);
        List<MonitorCheck> checks = checkRepository.findAllByMonitorIdAndCheckedAtBetweenOrderByCheckedAtAsc(monitorId, from, to);
        long failures = checks.stream().filter(check -> !check.isSuccess()).count();
        long incidents = incidentRepository.countByMonitorIdAndStartedAtBetween(monitorId, from, to);
        List<MonitorDtos.CheckResponse> recentChecks = checkRepository.findTop20ByMonitorIdOrderByCheckedAtDesc(monitorId).stream()
                .map(this::toCheckResponse)
                .toList();
        return new MonitorDtos.MetricsResponse(
                monitorId,
                range == null ? "24h" : range,
                MetricsCalculator.uptimePercentage(checks),
                MetricsCalculator.averageLatency(checks),
                MetricsCalculator.p95Latency(checks),
                failures,
                incidents,
                recentChecks
        );
    }

    public Monitor requireMonitor(UUID monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor not found"));
        accessControlService.requireProject(monitor.getProject().getId());
        return monitor;
    }

    public MonitorDtos.MonitorResponse toResponse(Monitor monitor) {
        return new MonitorDtos.MonitorResponse(
                monitor.getId(),
                monitor.getProject().getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getMethod(),
                monitor.getExpectedStatusCode(),
                monitor.getTimeoutSeconds(),
                monitor.getIntervalSeconds(),
                monitor.getLatencyThresholdMs(),
                monitor.getFailureThreshold(),
                monitor.getConsecutiveFailures(),
                monitor.isEnabled(),
                monitor.getState(),
                HeaderPolicy.redact(readHeaders(monitor.getHeadersJson())),
                monitor.getBody(),
                monitor.getLastCheckedAt(),
                monitor.getCreatedAt(),
                monitor.getUpdatedAt()
        );
    }

    public MonitorDtos.CheckResponse toCheckResponse(MonitorCheck check) {
        return new MonitorDtos.CheckResponse(
                check.getId(),
                check.getMonitor().getId(),
                check.getCheckedAt(),
                check.getLatencyMs(),
                check.getStatusCode(),
                check.isSuccess(),
                check.getErrorCategory(),
                check.getResponseSnippet()
        );
    }

    public Map<String, String> readHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(headersJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BadRequestException("Monitor headers are not valid JSON");
        }
    }

    private ValidatedMonitorInput validate(MonitorDtos.MonitorRequest request) {
        URI uri;
        try {
            uri = URI.create(request.url());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("url must be a valid HTTP or HTTPS URL");
        }
        if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            throw new BadRequestException("url must be an HTTP or HTTPS URL");
        }
        Map<String, String> headers = request.headers() == null ? Map.of() : request.headers();
        try {
            return new ValidatedMonitorInput(
                    request.name().trim(),
                    uri.toString(),
                    request.method(),
                    request.expectedStatusCode() == null ? 200 : request.expectedStatusCode(),
                    request.timeoutSeconds() == null ? 5 : request.timeoutSeconds(),
                    request.intervalSeconds() == null ? 300 : request.intervalSeconds(),
                    request.latencyThresholdMs() == null ? 1000 : request.latencyThresholdMs(),
                    request.failureThreshold() == null ? 3 : request.failureThreshold(),
                    objectMapper.writeValueAsString(headers),
                    request.body(),
                    request.enabled() == null || request.enabled()
            );
        } catch (Exception exception) {
            throw new BadRequestException("headers must be serializable as JSON");
        }
    }

    private record ValidatedMonitorInput(
            String name,
            String url,
            MonitorHttpMethod method,
            int expectedStatusCode,
            int timeoutSeconds,
            int intervalSeconds,
            int latencyThresholdMs,
            int failureThreshold,
            String headersJson,
            String body,
            boolean enabled
    ) {
    }
}
