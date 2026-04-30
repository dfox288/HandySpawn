package dev.handy.mods.handyspawn.state;

/**
 * Tracks the player's {@code time_since_rest} stat on the client and classifies it into
 * coarse phases that the HUD renders.
 *
 * <p>The vanilla mechanic: once a player has gone {@value #PHANTOM_THRESHOLD_TICKS} ticks
 * (three in-game days) without sleeping, phantoms may spawn at night. The counter lives
 * server-side and is pushed to clients via award-stats packets (on join, after sleep, and
 * via explicit stat-request replies).
 *
 * <p>The phase-calculation surface ({@link #phaseFor(int, int)} and
 * {@link #ticksRemainingUntilActive(int)}) is pure and unit-tested. The instance surface
 * stores the most recent observed value and a tick counter so the client module can throttle
 * outbound stat requests.
 */
public final class PhantomTimer {

    public static final int PHANTOM_THRESHOLD_TICKS = 72000;

    public enum Phase { SILENT, WARNING, ACTIVE }

    public static Phase phaseFor(int timeSinceRest, int warningStartTick) {
        if (timeSinceRest >= PHANTOM_THRESHOLD_TICKS) return Phase.ACTIVE;
        if (timeSinceRest >= warningStartTick) return Phase.WARNING;
        return Phase.SILENT;
    }

    public static int ticksRemainingUntilActive(int timeSinceRest) {
        return Math.max(0, PHANTOM_THRESHOLD_TICKS - timeSinceRest);
    }

    // Runtime polling surface
    private int timeSinceRest;
    private int tickCounter;

    public int timeSinceRest() { return timeSinceRest; }

    public void updateFromStats(int value) { this.timeSinceRest = Math.max(0, value); }

    public boolean shouldRequestStatsThisTick(int pollIntervalTicks) {
        tickCounter = (tickCounter + 1) % pollIntervalTicks;
        return tickCounter == 0;
    }

    private PhantomTimer() {}

    public static PhantomTimer create() { return new PhantomTimer(); }
}
