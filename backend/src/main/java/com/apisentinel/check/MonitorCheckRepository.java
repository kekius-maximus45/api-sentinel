package com.apisentinel.check;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, UUID> {
    Optional<MonitorCheck> findTopByMonitorIdOrderByCheckedAtDesc(UUID monitorId);

    List<MonitorCheck> findTop20ByMonitorIdOrderByCheckedAtDesc(UUID monitorId);

    List<MonitorCheck> findAllByMonitorIdAndCheckedAtBetweenOrderByCheckedAtAsc(UUID monitorId, Instant from, Instant to);
}
