package com.apisentinel.monitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    List<Monitor> findAllByProjectId(UUID projectId);

    List<Monitor> findAllByEnabledTrue();
}
