package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawnConstants;

/**
 * Single entry point for all HandySpawn world-space rendering.
 *
 * <p>Design intent: delegate one per-frame callback to five per-feature renderers
 * (house icon, bed tint, ghost marker, world-spawn marker, forceload border), each
 * of which calls into {@link RenderPrimitives}.
 *
 * <p><b>Runtime wiring is deferred.</b> In older Fabric API versions this would hook
 * into {@code WorldRenderEvents.AFTER_ENTITIES}, but that event was removed in
 * {@code fabric-rendering-v1 23.0.5+08c26e364c} (MC 26.1.2). Until a follow-up task
 * adds the replacement wiring (most likely a mixin into
 * {@code LevelRenderer.renderLevel}), {@link #register()} is a no-op and the five
 * per-feature renderers are never invoked.
 *
 * <p>Tasks 12–16 should still implement their {@code render(RenderContext)} bodies
 * against {@link RenderPrimitives}; once the runtime wiring lands, they will start
 * drawing with no further code change.
 */
public final class WorldRenderer {

    public static void register() {
        HandySpawnConstants.LOG.info(
            "HandySpawn WorldRenderer registered (runtime wiring pending: no "
            + "WorldRenderEvents equivalent in fabric-rendering-v1 for MC 26.1.2)");
    }

    /** Invoked once per frame by the eventual runtime wiring. */
    static void tick(RenderContext ctx) {
        HouseIconRenderer.render(ctx);
        BedTintRenderer.render(ctx);
        GhostMarkerRenderer.render(ctx);
        WorldSpawnRenderer.render(ctx);
        ForceloadBorderRenderer.render(ctx);
    }

    private WorldRenderer() {}
}
