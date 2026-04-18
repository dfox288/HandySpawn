package dev.handyspawn.render.world;

/**
 * Single entry point for all HandySpawn world-space rendering.
 *
 * <p>Invoked once per frame from {@code dev.handyspawn.mixin.LevelRendererMixin}
 * (an {@code @At("RETURN")} injector on {@code LevelRenderer.renderLevel}). Dispatches
 * to five per-feature renderers: house icon, bed tint, ghost marker, world-spawn
 * marker, and forceload border — each of which calls into {@link RenderPrimitives}.
 *
 * <p>This class deliberately has no {@code register()} method anymore: the mixin
 * is the entry point, so there is nothing to wire up at {@code onInitializeClient}.
 */
public final class WorldRenderer {

    /** Invoked once per frame by the {@code LevelRenderer} mixin. */
    public static void tick(RenderContext ctx) {
        HouseIconRenderer.render(ctx);
        BedTintRenderer.render(ctx);
        GhostMarkerRenderer.render(ctx);
        WorldSpawnRenderer.render(ctx);
        ForceloadBorderRenderer.render(ctx);
    }

    private WorldRenderer() {}
}
