package com.apisentinel.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findFirstByMonitorIdAndStatus(UUID monitorId, IncidentStatus status);

    List<Incident> findAllByProjectIdOrderByStartedAtDesc(UUID projectId);

    List<Incident> findAllByProjectIdAndStatusOrderByStartedAtDesc(UUID projectId, IncidentStatus status);

    List<Incident> findAllByProjectIdAndStatusAndResolvedAtAfterOrderByResolvedAtDesc(UUID projectId, IncidentStatus status, Instant resolvedAfter);

    long countByMonitorIdAndStartedAtBetween(UUID monitorId, Instant from, Instant to);
}
