package com.apisentinel.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class AlertDtos {
    private AlertDtos() {
    }

    public record AlertRuleRequest(
            UUID monitorId,
            UUID notificationChannelId,
            @NotBlank String name,
            @NotNull AlertCondition condition,
            Integer thresholdMs,
            Boolean enabled
    ) {
    }

    public record AlertRuleResponse(
            UUID id,
            UUID projectId,
            UUID monitorId,
            UUID notificationChannelId,
            String name,
            AlertCondition condition,
            Integer thresholdMs,
            boolean enabled,
            Instant createdAt
    ) {
    }

    public record ChannelRequest(
            @NotBlank String name,
            @NotBlank String webhookUrl
    ) {
    }

    public record ChannelResponse(
            UUID id,
            UUID projectId,
            String name,
            NotificationChannelType type,
            String webhookUrl,
            Instant createdAt
    ) {
    }
}
