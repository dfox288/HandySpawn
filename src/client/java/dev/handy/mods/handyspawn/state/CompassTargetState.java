package dev.handy.mods.handyspawn.state;

import java.util.Optional;

/**
 * Client-side tracking of the compass target (the global compass target /
 * {@code Level.getRespawnData()}) and, where detectable, the location of a
 * former target that has since been destroyed (e.g. the bed or respawn anchor
 * it pointed at has been mined).
 *
 * <p>Invariant: {@link #current()} and {@link #destroyedAt()} are never both
 * populated simultaneously.
 */
public final class CompassTargetState {

    private CompassTargetRecord current;
    private CompassTargetRecord destroyed;

    public Optional<CompassTargetRecord> current() {
        return Optional.ofNullable(current);
    }

    public Optional<CompassTargetRecord> destroyedAt() {
        return Optional.ofNullable(destroyed);
    }

    public void setCurrent(CompassTargetRecord record) {
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
