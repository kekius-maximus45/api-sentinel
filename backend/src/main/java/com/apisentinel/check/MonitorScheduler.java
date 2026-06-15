package com.apisentinel.check;

import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitorScheduler {
    private final MonitorRepository monitorRepository;
    private final CheckService checkService;

    // Tracks monitors that are currently being checked to prevent pile-up
    // if a check takes longer than the scheduler interval.
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public MonitorScheduler(MonitorRepository monitorRepository, CheckService checkService) {
        this.monitorRepository = monitorRepository;
        this.checkService = checkService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void dispatchDueChecks() {
        Instant now = Instant.now();
        for (Monitor monitor : monitorRepository.findAllByEnabledTrue()) {
            boolean due = monitor.getLastCheckedAt() == null
                    || monitor.getLastCheckedAt().plusSeconds(monitor.getIntervalSeconds()).isBefore(now);
            if (due && inFlight.add(monitor.getId())) {
                checkService.runScheduled(monitor.getId())
                        .whenComplete((result, ex) -> inFlight.remove(monitor.getId()));
            }
        }
    }
}
