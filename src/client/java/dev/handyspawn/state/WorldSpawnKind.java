package dev.handyspawn.state;

/**
 * Kind of block at a tracked spawn location. Only meaningful when the spawn sits
 * on a bed or a charged respawn anchor; world-spawn set via {@code /setworldspawn}
 * lands on arbitrary blocks and is recorded as {@link #BED} by convention for
 * display purposes.
 */
public enum WorldSpawnKind { BED, RESPAWN_ANCHOR }
