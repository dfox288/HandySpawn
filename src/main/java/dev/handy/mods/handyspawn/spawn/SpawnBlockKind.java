package dev.handy.mods.handyspawn.spawn;

/**
 * Block kind at a tracked spawn location, used by both server-synced
 * personal spawns ({@code PersonalSpawnPayload}) and client-tracked
 * compass targets ({@code CompassTargetRecord}).
 *
 * <p>The wire codec in {@code PersonalSpawnPayload} reads/writes by
 * ordinal index, so do not reorder existing values without bumping the
 * payload version.
 *
 * <p>{@link #BED} is also used as a generic fallback for compass targets
 * sitting on arbitrary blocks (world spawn set via {@code /setworldspawn})
 * because the surrounding UI only branches on the bed/anchor distinction.
 */
public enum SpawnBlockKind {
    BED,
    RESPAWN_ANCHOR
}
