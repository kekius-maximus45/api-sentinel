package com.apisentinel.monitor;

import com.apisentinel.check.MonitorCheck;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCalculatorTest {

    @Test
    void calculatesUptimeAverageAndP95Latency() {
        List<MonitorCheck> checks = List.of(
                check(100, true),
                check(200, true),
                check(500, false),
                check(900, true)
        );

        assertThat(MetricsCalculator.uptimePercentage(checks)).isEqualTo(75.0d);
        assertThat(MetricsCalculator.averageLatency(checks)).isEqualTo(425.0d);
        assertThat(MetricsCalculator.p95Latency(checks)).isEqualTo(900.0d);
    }

    @Test
    void emptyCheckWindowDefaultsToHealthyUptime() {
        assertThat(MetricsCalculator.uptimePercentage(List.of())).isEqualTo(100.0d);
        assertThat(MetricsCalculator.averageLatency(List.of())).isZero();
        assertThat(MetricsCalculator.p95Latency(List.of())).isZero();
    }

    private MonitorCheck check(int latencyMs, boolean success) {
        return new MonitorCheck(null, Instant.now(), latencyMs, success ? 200 : 500, success, success ? null : "UNEXPECTED_STATUS", null);
    }
}
