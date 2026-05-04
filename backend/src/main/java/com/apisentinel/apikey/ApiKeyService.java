package com.apisentinel.apikey;

import com.apisentinel.common.ForbiddenException;
import com.apisentinel.organization.AccessControlService;
import com.apisentinel.organization.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final AccessControlService accessControlService;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AccessControlService accessControlService) {
        this.apiKeyRepository = apiKeyRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDtos.ApiKeyResponse> list(UUID organizationId) {
        accessControlService.requireOrganization(organizationId);
        return apiKeyRepository.findAllByOrganizationId(organizationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApiKeyDtos.ApiKeyCreateResponse create(ApiKeyDtos.ApiKeyRequest request) {
        Organization organization = accessControlService.requireWritableOrganization(request.organizationId()).getOrganization();
        String secret = generateSecret();
        String prefix = secret.substring(0, 12);
        ApiKey apiKey = apiKeyRepository.save(new ApiKey(organization, request.name().trim(), prefix, hash(secret)));
        return new ApiKeyDtos.ApiKeyCreateResponse(toResponse(apiKey), secret);
    }

    @Transactional
    public ApiKey authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new ForbiddenException("API key is required");
        }
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hash(rawKey))
                .orElseThrow(() -> new ForbiddenException("Invalid API key"));
        apiKey.markUsed(Instant.now());
        return apiKey;
    }

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash API key", exception);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "aps_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKeyDtos.ApiKeyResponse toResponse(ApiKey apiKey) {
        return new ApiKeyDtos.ApiKeyResponse(
                apiKey.getId(),
                apiKey.getOrganization().getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt()
        );
    }
}
