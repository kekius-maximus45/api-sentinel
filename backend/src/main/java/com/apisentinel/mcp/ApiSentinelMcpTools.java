package com.apisentinel.mcp;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ApiSentinelMcpTools {
    private final McpToolService mcpToolService;

    public ApiSentinelMcpTools(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @McpTool(name = "get_monitor_health", description = "Get current health and latest check data for a monitor.")
    public Map<String, Object> getMonitorHealth(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Monitor UUID", required = true) String monitorId
    ) {
        return mcpToolService.monitorHealth(apiKey, UUID.fromString(monitorId));
    }

    @McpTool(name = "get_incident_history", description = "Get recent incidents for a project.")
    public Object getIncidentHistory(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Project UUID", required = true) String projectId
    ) {
        return mcpToolService.incidentHistory(apiKey, UUID.fromString(projectId));
    }

    @McpTool(name = "get_latency_stats", description = "Get uptime, average latency, p95 latency, and failure count for a monitor.")
    public Map<String, Object> getLatencyStats(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Monitor UUID", required = true) String monitorId,
            @McpToolParam(description = "Range: 24h, 7d, or 30d", required = false) String range
    ) {
        return mcpToolService.latencyStats(apiKey, UUID.fromString(monitorId), range);
    }

    @McpTool(name = "summarize_recent_incidents", description = "Summarize recent incidents for a project.")
    public String summarizeRecentIncidents(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Project UUID", required = true) String projectId
    ) {
        return mcpToolService.summarizeRecentIncidents(apiKey, UUID.fromString(projectId));
    }

    @McpTool(name = "draft_status_update", description = "Draft a customer-facing status update for a monitor.")
    public String draftStatusUpdate(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Monitor UUID", required = true) String monitorId
    ) {
        return mcpToolService.draftStatusUpdate(apiKey, UUID.fromString(monitorId));
    }

    @McpTool(name = "create_alert_rule", description = "Create an alert rule. Requires confirmed=true.")
    public Map<String, Object> createAlertRule(
            @McpToolParam(description = "API Sentinel API key", required = true) String apiKey,
            @McpToolParam(description = "Project UUID", required = true) String projectId,
            @McpToolParam(description = "Optional monitor UUID", required = false) String monitorId,
            @McpToolParam(description = "Optional notification channel UUID", required = false) String notificationChannelId,
            @McpToolParam(description = "Rule name", required = true) String name,
            @McpToolParam(description = "MONITOR_DOWN, MONITOR_RECOVERED, or LATENCY_THRESHOLD_EXCEEDED", required = true) String condition,
            @McpToolParam(description = "Optional threshold in milliseconds", required = false) Integer thresholdMs,
            @McpToolParam(description = "Explicit confirmation for configuration changes", required = true) boolean confirmed
    ) {
        return mcpToolService.createAlertRule(
                apiKey,
                UUID.fromString(projectId),
                monitorId == null || monitorId.isBlank() ? null : UUID.fromString(monitorId),
                notificationChannelId == null || notificationChannelId.isBlank() ? null : UUID.fromString(notificationChannelId),
                name,
                condition,
                thresholdMs,
                confirmed
        );
    }
}
