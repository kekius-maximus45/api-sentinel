package com.apisentinel.monitor;

import com.apisentinel.check.CheckService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MonitorController {
    private final MonitorService monitorService;
    private final CheckService checkService;

    public MonitorController(MonitorService monitorService, CheckService checkService) {
        this.monitorService = monitorService;
        this.checkService = checkService;
    }

    @GetMapping("/api/projects/{projectId}/monitors")
    public List<MonitorDtos.MonitorResponse> list(@PathVariable UUID projectId) {
        return monitorService.list(projectId);
    }

    @PostMapping("/api/projects/{projectId}/monitors")
    public MonitorDtos.MonitorResponse create(@PathVariable UUID projectId, @Valid @RequestBody MonitorDtos.MonitorRequest request) {
        return monitorService.create(projectId, request);
    }

    @GetMapping("/api/monitors/{monitorId}")
    public MonitorDtos.MonitorResponse get(@PathVariable UUID monitorId) {
        return monitorService.get(monitorId);
    }

    @PutMapping("/api/monitors/{monitorId}")
    public MonitorDtos.MonitorResponse update(@PathVariable UUID monitorId, @Valid @RequestBody MonitorDtos.MonitorRequest request) {
        return monitorService.update(monitorId, request);
    }

    @DeleteMapping("/api/monitors/{monitorId}")
    public void delete(@PathVariable UUID monitorId) {
        monitorService.delete(monitorId);
    }

    @PostMapping("/api/monitors/{monitorId}/pause")
    public MonitorDtos.MonitorResponse pause(@PathVariable UUID monitorId) {
        return monitorService.pause(monitorId);
    }

    @PostMapping("/api/monitors/{monitorId}/resume")
    public MonitorDtos.MonitorResponse resume(@PathVariable UUID monitorId) {
        return monitorService.resume(monitorId);
    }

    @GetMapping("/api/monitors/{monitorId}/checks")
    public List<MonitorDtos.CheckResponse> checks(@PathVariable UUID monitorId) {
        return monitorService.checks(monitorId);
    }

    @GetMapping("/api/monitors/{monitorId}/metrics")
    public MonitorDtos.MetricsResponse metrics(@PathVariable UUID monitorId, @RequestParam(defaultValue = "24h") String range) {
        return monitorService.metrics(monitorId, range);
    }

    @PostMapping("/api/monitors/{monitorId}/run-check")
    public MonitorDtos.CheckResponse runCheck(@PathVariable UUID monitorId) {
        return checkService.runManual(monitorId);
    }
}
