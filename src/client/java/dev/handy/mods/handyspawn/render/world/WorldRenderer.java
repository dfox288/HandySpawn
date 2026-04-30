package dev.handy.mods.handyspawn.render.world;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

/**
 * Single entry point for all HandySpawn world-space rendering.
 *
 * <p>Registered via {@link LevelRenderEvents#AFTER_TRANSLUCENT_TERRAIN}, which is
 * the replacement for the classic {@code WorldRenderEvents.AFTER_ENTITIES}: it
 * fires inside {@code LevelRenderer.renderLevel} while vanilla's PoseStack is
 * still alive (view matrix applied, camera-relative) and the BufferSource is
 * still accepting vertices. Drawing at {@code @At("RETURN")} of renderLevel (as
 * we previously did with a custom mixin) put us after vanilla's end-batch
 * calls, so our vertices silently never reached the GPU.
 *
 * <p>Dispatches to five per-feature renderers: house icon, bed tint, ghost
 * marker, world-spawn marker, and forceload border — each of which calls into
 * {@link RenderPrimitives}.
 */
public final class WorldRenderer {

    public static void register() {
        // COLLECT_SUBMITS is the phase during which BlockEntityRenderers and
        // other submit-based content queue their draws (see HandyBookshelf's
        // ChiseledBookshelfRenderer for the proven pattern). Firing later —
        // e.g. AFTER_TRANSLUCENT_TERRAIN or BEFORE_GIZMOS — lands after the
        // collector has already been drained, so nothing renders.
        LevelRenderEvents.COLLECT_SUBMITS.register(fabricCtx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            RenderContext ctx = new RenderContext(
                fabricCtx.poseStack(),
                mc.gameRenderer.mainCamera(),
                fabricCtx.levelState().cameraRenderState.orientation,
                fabricCtx.submitNodeCollector(),
                mc.level,
                mc.getDeltaTracker().getGameTimeDeltaTicks()
            );
            HouseIconRenderer.render(ctx);
            BedTintRenderer.render(ctx);
            GhostMarkerRenderer.render(ctx);
            WorldSpawnRenderer.render(ctx);
            ForceloadBorderRenderer.render(ctx);
        });
    }

    private WorldRenderer() {}
}
