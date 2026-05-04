package com.apisentinel.incident;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/api/projects/{projectId}/incidents")
    public List<IncidentDtos.IncidentResponse> list(@PathVariable UUID projectId) {
        return incidentService.list(projectId);
    }

    @GetMapping("/api/incidents/{incidentId}")
    public IncidentDtos.IncidentResponse get(@PathVariable UUID incidentId) {
        return incidentService.get(incidentId);
    }

    @PostMapping("/api/incidents/{incidentId}/notes")
    public IncidentDtos.IncidentResponse addNote(@PathVariable UUID incidentId, @Valid @RequestBody IncidentDtos.NoteRequest request) {
        return incidentService.addNote(incidentId, request.message());
    }

    @PostMapping("/api/incidents/{incidentId}/resolve")
    public IncidentDtos.IncidentResponse resolve(@PathVariable UUID incidentId) {
        return incidentService.resolve(incidentId);
    }
}
