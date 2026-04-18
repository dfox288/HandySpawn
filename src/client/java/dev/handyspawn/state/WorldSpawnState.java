package dev.handyspawn.state;

import java.util.Optional;

/**
 * Client-side tracking of the world spawn and, where detectable, the location
 * of a former world spawn that has since been destroyed (e.g. the bed or
 * respawn anchor it pointed at has been mined).
 *
 * <p>Invariant: {@link #current()} and {@link #destroyedAt()} are never both
 * populated simultaneously.
 */
public final class WorldSpawnState {

    private WorldSpawnRecord current;
    private WorldSpawnRecord destroyed;

    public Optional<WorldSpawnRecord> current() {
        return Optional.ofNullable(current);
    }

    public Optional<WorldSpawnRecord> destroyedAt() {
        return Optional.ofNullable(destroyed);
    }

    public void setCurrent(WorldSpawnRecord record) {
        this.current = record;
        this.destroyed = null;
    }

    public void clearCurrent() {
        if (this.current != null) {
            this.destroyed = this.current;
        }
        this.current = null;
    }
}
