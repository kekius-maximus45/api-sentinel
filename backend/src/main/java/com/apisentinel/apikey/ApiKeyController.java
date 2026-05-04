package com.apisentinel.apikey;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping("/api/api-keys")
    public List<ApiKeyDtos.ApiKeyResponse> list(@RequestParam UUID organizationId) {
        return apiKeyService.list(organizationId);
    }

    @PostMapping("/api/api-keys")
    public ApiKeyDtos.ApiKeyCreateResponse create(@Valid @RequestBody ApiKeyDtos.ApiKeyRequest request) {
        return apiKeyService.create(request);
    }
}
