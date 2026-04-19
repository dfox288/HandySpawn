package dev.handyspawn.state;

import dev.handyspawn.net.PersonalSpawnKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Client-side tracking of the local player's personal spawn (bed or charged
 * respawn anchor). Populated solely from
 * {@link dev.handyspawn.net.PersonalSpawnPayload} broadcasts — there is no
 * local fallback, because MC 26.1.2 does not sync {@code ServerPlayer.respawnConfig}
 * to the client through vanilla channels.
 *
 * <p>Invariant: {@link #current()} and {@link #destroyedAt()} are never both
 * populated simultaneously. A fresh "here is your spawn" payload clears any
 * destroyed marker; a "your spawn was destroyed" payload moves the old record
 * into the destroyed slot; an empty payload clears both.
 */
public final class PersonalSpawnState {

    public record Record(BlockPos pos, ResourceKey<Level> dimension, PersonalSpawnKind kind) {}

    private Record current;
    private Record destroyed;

    public Optional<Record> current() {
        return Optional.ofNullable(current);
    }

    public Optional<Record> destroyedAt() {
        return Optional.ofNullable(destroyed);
    }

    public void applyPayload(Optional<BlockPos> pos, Optional<ResourceKey<Level>> dim,
                             PersonalSpawnKind kind, boolean isDestroyed) {
        if (isDestroyed && pos.isPresent() && dim.isPresent()) {
            destroyed = new Record(pos.get(), dim.get(), kind);
            current = null;
        } else if (pos.isPresent() && dim.isPresent()) {
            current = new Record(pos.get(), dim.get(), kind);
            destroyed = null;
        } else {
            // Empty payload — the player has no current spawn. Preserve any
            // destroyed record the client may have just latched: a
            // destroyed=true broadcast followed by an empty one is the
            // expected sequence when vanilla clears the respawn config after
            // a bed break, and clobbering `destroyed` here would prevent the
            // ghost marker from ever rendering. A fresh "current" payload
            // (branch above) is what properly clears the destroyed slot.
            current = null;
        }
        // DIAG (bed-break HOME-persists bug, 2026-04-19): log resulting state
        // on every payload so we can correlate renderer observations with state.
        dev.handyspawn.HandySpawnConstants.LOG.info(
            "[apply] rx pos={} destroyed_flag={} => current={} destroyed={}",
            pos.map(Object::toString).orElse("empty"),
            isDestroyed,
            current == null ? "null" : current.pos().toString(),
            destroyed == null ? "null" : destroyed.pos().toString());
    }

    public void clear() {
        current = null;
        destroyed = null;
    }
}
