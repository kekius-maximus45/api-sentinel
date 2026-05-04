package com.apisentinel.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AiController {
    private final AiIncidentAssistantService assistantService;

    public AiController(AiIncidentAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/ai/incident-assistant/chat")
    public AiDtos.ChatResponse chat(@Valid @RequestBody AiDtos.ChatRequest request) {
        return assistantService.chat(request);
    }

    @GetMapping("/api/ai/audit-logs")
    public List<AiDtos.AuditLogResponse> auditLogs(@RequestParam UUID organizationId) {
        return assistantService.auditLogs(organizationId);
    }
}
