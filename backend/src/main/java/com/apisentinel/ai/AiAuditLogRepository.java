package com.apisentinel.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, UUID> {
    List<AiAuditLog> findTop100ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
