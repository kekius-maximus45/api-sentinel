package com.apisentinel.alert;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/api/projects/{projectId}/alert-rules")
    public List<AlertDtos.AlertRuleResponse> listRules(@PathVariable UUID projectId) {
        return alertService.listRules(projectId);
    }

    @PostMapping("/api/projects/{projectId}/alert-rules")
    public AlertDtos.AlertRuleResponse createRule(@PathVariable UUID projectId, @Valid @RequestBody AlertDtos.AlertRuleRequest request) {
        return alertService.createRule(projectId, request);
    }

    @PutMapping("/api/alert-rules/{ruleId}")
    public AlertDtos.AlertRuleResponse updateRule(@PathVariable UUID ruleId, @Valid @RequestBody AlertDtos.AlertRuleRequest request) {
        return alertService.updateRule(ruleId, request);
    }

    @DeleteMapping("/api/alert-rules/{ruleId}")
    public void deleteRule(@PathVariable UUID ruleId) {
        alertService.deleteRule(ruleId);
    }

    @GetMapping("/api/projects/{projectId}/notification-channels")
    public List<AlertDtos.ChannelResponse> listChannels(@PathVariable UUID projectId) {
        return alertService.listChannels(projectId);
    }

    @PostMapping("/api/projects/{projectId}/notification-channels")
    public AlertDtos.ChannelResponse createChannel(@PathVariable UUID projectId, @Valid @RequestBody AlertDtos.ChannelRequest request) {
        return alertService.createChannel(projectId, request);
    }

    @PostMapping("/api/notification-channels/{channelId}/test")
    public void testChannel(@PathVariable UUID channelId) {
        alertService.testChannel(channelId);
    }
}
