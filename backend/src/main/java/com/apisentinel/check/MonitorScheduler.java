package com.apisentinel.check;

import com.apisentinel.monitor.Monitor;
import com.apisentinel.monitor.MonitorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MonitorScheduler {
    private final MonitorRepository monitorRepository;
    private final CheckService checkService;

    public MonitorScheduler(MonitorRepository monitorRepository, CheckService checkService) {
        this.monitorRepository = monitorRepository;
        this.checkService = checkService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void dispatchDueChecks() {
        Instant now = Instant.now();
        for (Monitor monitor : monitorRepository.findAllByEnabledTrue()) {
            if (monitor.getLastCheckedAt() == null || monitor.getLastCheckedAt().plusSeconds(monitor.getIntervalSeconds()).isBefore(now)) {
                checkService.runScheduled(monitor.getId());
            }
        }
    }
}
