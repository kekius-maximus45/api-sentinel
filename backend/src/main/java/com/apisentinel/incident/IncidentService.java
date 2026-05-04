package com.apisentinel.incident;

import com.apisentinel.alert.AlertCondition;
import com.apisentinel.alert.AlertService;
import com.apisentinel.auth.CurrentUserService;
import com.apisentinel.check.MonitorCheck;
import com.apisentinel.common.NotFoundException;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorState;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.organization.UserAccount;
import com.apisentinel.organization.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository eventRepository;
    private final AccessControlService accessControlService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AlertService alertService;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentEventRepository eventRepository,
            AccessControlService accessControlService,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            AlertService alertService
    ) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.accessControlService = accessControlService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.alertService = alertService;
    }

    @Transactional
    public void handleMonitorCheck(Monitor monitor, MonitorCheck check, MonitorState previousState) {
        var activeIncident = incidentRepository.findFirstByMonitorIdAndStatus(monitor.getId(), IncidentStatus.OPEN);
        if (!check.isSuccess() && monitor.getState() == MonitorState.DOWN) {
            Incident incident = activeIncident.orElseGet(() -> openIncident(monitor, check.getCheckedAt()));
            addEvent(incident, IncidentEventType.CHECK_FAILED, "Check failed: " + check.getErrorCategory(), null, null);
            if (activeIncident.isEmpty()) {
                alertService.dispatch(monitor, incident, AlertCondition.MONITOR_DOWN, check.getLatencyMs());
            }
            return;
        }
        if (check.isSuccess() && activeIncident.isPresent()) {
            Incident incident = activeIncident.get();
            incident.resolve(check.getCheckedAt());
            addEvent(incident, IncidentEventType.RESOLVED, "Monitor recovered", null, null);
            alertService.dispatch(monitor, incident, AlertCondition.MONITOR_RECOVERED, check.getLatencyMs());
            return;
        }
        if (check.isSuccess() && check.getLatencyMs() != null && check.getLatencyMs() > monitor.getLatencyThresholdMs()) {
            alertService.dispatch(monitor, activeIncident.orElse(null), AlertCondition.LATENCY_THRESHOLD_EXCEEDED, check.getLatencyMs());
        }
        if (previousState == MonitorState.DOWN && monitor.getState() == MonitorState.UP) {
            activeIncident.ifPresent(incident -> alertService.dispatch(monitor, incident, AlertCondition.MONITOR_RECOVERED, check.getLatencyMs()));
        }
    }

    @Transactional(readOnly = true)
    public List<IncidentDtos.IncidentResponse> list(UUID projectId) {
        accessControlService.requireProject(projectId);
        return incidentRepository.findAllByProjectIdOrderByStartedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDtos.IncidentResponse get(UUID incidentId) {
        return toResponse(requireIncident(incidentId));
    }

    @Transactional
    public IncidentDtos.IncidentResponse addNote(UUID incidentId, String message) {
        Incident incident = requireIncident(incidentId);
        UserAccount user = userRepository.findById(currentUserService.currentUser().id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        addEvent(incident, IncidentEventType.NOTE_ADDED, message.trim(), null, user);
        return toResponse(incident);
    }

    @Transactional
    public IncidentDtos.IncidentResponse resolve(UUID incidentId) {
        Incident incident = requireIncident(incidentId);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            incident.resolve(Instant.now());
            addEvent(incident, IncidentEventType.RESOLVED, "Incident resolved manually", null, null);
            alertService.dispatch(incident.getMonitor(), incident, AlertCondition.MONITOR_RECOVERED, null);
        }
        return toResponse(incident);
    }

    public void addAlertEvent(Incident incident, String message, String metadataJson) {
        if (incident != null) {
            addEvent(incident, IncidentEventType.ALERT_SENT, message, metadataJson, null);
        }
    }

    private Incident openIncident(Monitor monitor, Instant startedAt) {
        Incident incident = incidentRepository.save(new Incident(
                monitor.getProject(),
                monitor,
                monitor.getName() + " is down",
                startedAt
        ));
        addEvent(incident, IncidentEventType.CREATED, "Incident created after consecutive check failures", null, null);
        return incident;
    }

    private Incident requireIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
        accessControlService.requireProject(incident.getProject().getId());
        return incident;
    }

    private void addEvent(Incident incident, IncidentEventType type, String message, String metadataJson, UserAccount createdBy) {
        eventRepository.save(new IncidentEvent(incident, type, message, metadataJson, createdBy));
    }

    public IncidentDtos.IncidentResponse toResponse(Incident incident) {
        List<IncidentDtos.EventResponse> events = eventRepository.findAllByIncidentIdOrderByCreatedAtAsc(incident.getId()).stream()
                .map(event -> new IncidentDtos.EventResponse(
                        event.getId(),
                        event.getType(),
                        event.getMessage(),
                        event.getMetadataJson(),
                        event.getCreatedAt()
                ))
                .toList();
        return new IncidentDtos.IncidentResponse(
                incident.getId(),
                incident.getProject().getId(),
                incident.getMonitor().getId(),
                incident.getMonitor().getName(),
                incident.getTitle(),
                incident.getStatus(),
                incident.getStartedAt(),
                incident.getResolvedAt(),
                events
        );
    }
}
