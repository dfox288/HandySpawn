package dev.handy.mods.handyspawn.state;

import java.util.function.Consumer;

/**
 * Tracks the lifecycle of a single client play session and whether the
 * connected server has HandySpawn installed (determined by receipt of a
 * {@link dev.handy.mods.handyspawn.net.HandshakePayload}).
 *
 * <p>Renderers and HUDs read {@link #isServerSynced()} to decide whether to
 * draw from server-synced per-player state (via
 * {@link PersonalSpawnState}) or from the compass-target fallback
 * (via {@link CompassTargetState}). If the handshake hasn't arrived within
 * {@link #FALLBACK_AFTER_TICKS} ticks after joining, a one-shot message is
 * dispatched via the {@code onFallback} consumer so the mod can log that it
 * is operating in legacy / world-spawn-only mode.
 */
public final class SessionState {

    public static final int FALLBACK_AFTER_TICKS = 60;

    private boolean serverHasHandyspawn;
    private boolean inSession;
    private int ticksSinceJoin;
    private boolean fallbackLogged;

    public boolean isServerSynced() { return serverHasHandyspawn; }

    public boolean inSession() { return inSession; }

    public void onJoin() {
        inSession = true;
        serverHasHandyspawn = false;
        ticksSinceJoin = 0;
        fallbackLogged = false;
    }

    public void onHandshakeReceived() {
        if (inSession) {
            serverHasHandyspawn = true;
        }
    }

    public void onDisconnect() {
        inSession = false;
        serverHasHandyspawn = false;
        ticksSinceJoin = 0;
        fallbackLogged = false;
    }

    /**
     * Called once per client tick. If we've been in a session for more than
     * {@link #FALLBACK_AFTER_TICKS} without a handshake, invoke {@code onFallback}
     * exactly once.
     */
    public void onTick(Consumer<String> onFallback) {
        if (!inSession) return;
        if (serverHasHandyspawn) return;
        if (fallbackLogged) return;
        ticksSinceJoin++;
        if (ticksSinceJoin >= FALLBACK_AFTER_TICKS) {
            fallbackLogged = true;
            onFallback.accept("connected to a server without HandySpawn; falling back to world-spawn-only mode");
        }
    }
}
