package dev.handyspawn.net;

/**
 * Kind of block at a player's personal spawn location. Mirrors the existing
 * {@link dev.handyspawn.state.CompassTargetKind} but lives in the shared {@code net}
 * package so both server and client can reference it without pulling in
 * client-only state classes.
 */
public enum PersonalSpawnKind {
    BED,
    RESPAWN_ANCHOR
}
