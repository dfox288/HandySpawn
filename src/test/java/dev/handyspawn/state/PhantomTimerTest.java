package dev.handyspawn.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhantomTimerTest {

    @Test
    void warningPhaseStartsAtWarningStartTick() {
        PhantomTimer.Phase phase = PhantomTimer.phaseFor(48000, 48000);
        assertEquals(PhantomTimer.Phase.WARNING, phase);
    }

    @Test
    void belowWarningStartIsSilent() {
        assertEquals(PhantomTimer.Phase.SILENT, PhantomTimer.phaseFor(47999, 48000));
    }

    @Test
    void atThresholdIsActive() {
        assertEquals(PhantomTimer.Phase.ACTIVE, PhantomTimer.phaseFor(72000, 48000));
    }

    @Test
    void beyondThresholdIsActive() {
        assertEquals(PhantomTimer.Phase.ACTIVE, PhantomTimer.phaseFor(120000, 48000));
    }

    @Test
    void ticksRemainingInWarningPhase() {
        assertEquals(12000, PhantomTimer.ticksRemainingUntilActive(60000));
        assertEquals(24000, PhantomTimer.ticksRemainingUntilActive(48000));
        assertEquals(0, PhantomTimer.ticksRemainingUntilActive(72000));
        assertEquals(0, PhantomTimer.ticksRemainingUntilActive(80000));
    }
}
