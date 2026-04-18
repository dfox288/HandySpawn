package dev.handyspawn.state;

import java.util.Optional;

public final class SpawnState {

    private SpawnRecord current;
    private SpawnRecord destroyed;

    public Optional<SpawnRecord> current() {
        return Optional.ofNullable(current);
    }

    public Optional<SpawnRecord> destroyedAt() {
        return Optional.ofNullable(destroyed);
    }

    public void setCurrent(SpawnRecord record) {
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
