package com.apisentinel.check;

import com.apisentinel.monitor.MonitorState;

public record StateDecision(MonitorState state, int consecutiveFailures) {
}
