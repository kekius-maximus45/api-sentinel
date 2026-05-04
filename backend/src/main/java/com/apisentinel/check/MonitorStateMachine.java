package com.apisentinel.check;

import com.apisentinel.monitor.MonitorState;

public final class MonitorStateMachine {
    private MonitorStateMachine() {
    }

    public static StateDecision decide(
            boolean enabled,
            boolean success,
            Integer latencyMs,
            int latencyThresholdMs,
            int previousConsecutiveFailures,
            int failureThreshold
    ) {
        if (!enabled) {
            return new StateDecision(MonitorState.PAUSED, previousConsecutiveFailures);
        }
        if (success) {
            if (latencyMs != null && latencyMs > latencyThresholdMs) {
                return new StateDecision(MonitorState.DEGRADED, 0);
            }
            return new StateDecision(MonitorState.UP, 0);
        }
        int failures = previousConsecutiveFailures + 1;
        if (failures >= failureThreshold) {
            return new StateDecision(MonitorState.DOWN, failures);
        }
        return new StateDecision(MonitorState.DEGRADED, failures);
    }
}
