package com.apisentinel.auth;

import com.apisentinel.organization.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8, max = 120) String password,
            @NotBlank String displayName,
            @NotBlank String organizationName
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(String accessToken, UserProfile user, List<OrganizationSummary> organizations) {
    }

    public record UserProfile(UUID id, String email, String displayName) {
    }

    public record OrganizationSummary(UUID id, String name, OrganizationRole role) {
    }
}
