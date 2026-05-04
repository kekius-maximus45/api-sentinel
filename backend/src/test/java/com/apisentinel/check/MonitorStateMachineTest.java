package com.apisentinel.check;

import com.apisentinel.monitor.MonitorState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorStateMachineTest {

    @Test
    void marksPausedWhenDisabled() {
        StateDecision decision = MonitorStateMachine.decide(false, true, 100, 500, 0, 3);

        assertThat(decision.state()).isEqualTo(MonitorState.PAUSED);
    }

    @Test
    void marksDegradedForSlowSuccess() {
        StateDecision decision = MonitorStateMachine.decide(true, true, 900, 500, 2, 3);

        assertThat(decision.state()).isEqualTo(MonitorState.DEGRADED);
        assertThat(decision.consecutiveFailures()).isZero();
    }

    @Test
    void marksDownOnlyAfterFailureThreshold() {
        StateDecision degraded = MonitorStateMachine.decide(true, false, 100, 500, 1, 3);
        StateDecision down = MonitorStateMachine.decide(true, false, 100, 500, 2, 3);

        assertThat(degraded.state()).isEqualTo(MonitorState.DEGRADED);
        assertThat(degraded.consecutiveFailures()).isEqualTo(2);
        assertThat(down.state()).isEqualTo(MonitorState.DOWN);
        assertThat(down.consecutiveFailures()).isEqualTo(3);
    }
}
