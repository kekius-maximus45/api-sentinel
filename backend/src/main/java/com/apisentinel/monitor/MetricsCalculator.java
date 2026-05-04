package com.apisentinel.monitor;

import com.apisentinel.check.MonitorCheck;

import java.util.Comparator;
import java.util.List;

public final class MetricsCalculator {
    private MetricsCalculator() {
    }

    public static double uptimePercentage(List<MonitorCheck> checks) {
        if (checks.isEmpty()) {
            return 100.0d;
        }
        long successes = checks.stream().filter(MonitorCheck::isSuccess).count();
        return (successes * 100.0d) / checks.size();
    }

    public static double averageLatency(List<MonitorCheck> checks) {
        return checks.stream()
                .filter(check -> check.getLatencyMs() != null)
                .mapToInt(MonitorCheck::getLatencyMs)
                .average()
                .orElse(0.0d);
    }

    public static double p95Latency(List<MonitorCheck> checks) {
        List<Integer> latencies = checks.stream()
                .map(MonitorCheck::getLatencyMs)
                .filter(value -> value != null)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (latencies.isEmpty()) {
            return 0.0d;
        }
        int index = (int) Math.ceil(latencies.size() * 0.95d) - 1;
        return latencies.get(Math.max(0, Math.min(index, latencies.size() - 1)));
    }
}
