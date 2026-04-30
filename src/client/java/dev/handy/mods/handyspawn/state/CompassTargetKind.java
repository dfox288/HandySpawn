package dev.handy.mods.handyspawn.state;

/**
 * Kind of block at a tracked compass-target location. Only meaningful when the
 * target sits on a bed or a charged respawn anchor; world-spawn set via
 * {@code /setworldspawn} lands on arbitrary blocks and is recorded as
 * {@link #BED} by convention for display purposes.
 */
public enum CompassTargetKind { BED, RESPAWN_ANCHOR }
