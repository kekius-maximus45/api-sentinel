package com.apisentinel.alert;

import com.apisentinel.config.AppProperties;
import com.apisentinel.incident.Incident;
import com.apisentinel.monitor.Monitor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebhookPayloadFactory {
    private final AppProperties properties;

    public WebhookPayloadFactory(AppProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> build(Monitor monitor, Incident incident, AlertCondition condition, Integer latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", condition.name());
        payload.put("timestamp", Instant.now().toString());
        payload.put("dashboardUrl", properties.appUrl() + "/monitors/" + monitor.getId());
        payload.put("project", Map.of(
                "id", monitor.getProject().getId(),
                "name", monitor.getProject().getName()
        ));
        payload.put("monitor", Map.of(
                "id", monitor.getId(),
                "name", monitor.getName(),
                "state", monitor.getState().name(),
                "latencyMs", latencyMs == null ? "" : latencyMs
        ));
        if (incident != null) {
            payload.put("incident", Map.of(
                    "id", incident.getId(),
                    "status", incident.getStatus().name(),
                    "title", incident.getTitle()
            ));
        }
        return payload;
    }
}
