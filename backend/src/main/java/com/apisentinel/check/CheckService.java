package com.apisentinel.check;

import com.apisentinel.incident.IncidentService;
import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorDtos;
import com.apisentinel.monitor.MonitorRepository;
import com.apisentinel.monitor.MonitorService;
import com.apisentinel.monitor.MonitorState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CheckService {
    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository checkRepository;
    private final MonitorService monitorService;
    private final CheckExecutor checkExecutor;
    private final IncidentService incidentService;

    public CheckService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository checkRepository,
            MonitorService monitorService,
            CheckExecutor checkExecutor,
            IncidentService incidentService
    ) {
        this.monitorRepository = monitorRepository;
        this.checkRepository = checkRepository;
        this.monitorService = monitorService;
        this.checkExecutor = checkExecutor;
        this.incidentService = incidentService;
    }

    @Transactional
    public MonitorDtos.CheckResponse runManual(UUID monitorId) {
        Monitor monitor = monitorService.requireMonitor(monitorId);
        return executeAndPersist(monitor);
    }

    @Async
    @Transactional
    public void runScheduled(UUID monitorId) {
        monitorRepository.findById(monitorId).ifPresent(this::executeAndPersist);
    }

    private MonitorDtos.CheckResponse executeAndPersist(Monitor monitor) {
        MonitorState previousState = monitor.getState();
        CheckResult result = checkExecutor.execute(monitor, monitorService.readHeaders(monitor.getHeadersJson()));
        MonitorCheck check = checkRepository.save(new MonitorCheck(
                monitor,
                result.checkedAt(),
                result.latencyMs(),
                result.statusCode(),
                result.success(),
                result.errorCategory(),
                result.responseSnippet()
        ));
        StateDecision decision = MonitorStateMachine.decide(
                monitor.isEnabled(),
                result.success(),
                result.latencyMs(),
                monitor.getLatencyThresholdMs(),
                monitor.getConsecutiveFailures(),
                monitor.getFailureThreshold()
        );
        monitor.recordState(decision.state(), decision.consecutiveFailures(), result.checkedAt());
        incidentService.handleMonitorCheck(monitor, check, previousState);
        return monitorService.toCheckResponse(check);
    }
}
