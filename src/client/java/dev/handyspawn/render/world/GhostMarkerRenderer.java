package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * F3 feature: billboard a red ghost-house sprite at the coordinates where the
 * player's tracked spawn used to be. Always rendered through walls — a
 * destroyed spawn with something built on top is a core use case.
 *
 * <p>When the server has HandySpawn installed, sources from
 * {@link dev.handyspawn.state.PersonalSpawnState#destroyedAt()} (populated
 * server-side on bed/anchor destruction). Otherwise falls back to
 * {@link dev.handyspawn.state.CompassTargetState#destroyedAt()} which is
 * populated on chunk-load verification of the world spawn.
 */
public final class GhostMarkerRenderer {

    /** Same HOUSE glyph as {@link HouseIconRenderer} but red-tinted for "gone". */
    private static final String GLYPH = "\u2302";

    /** Dusky red; the alpha channel handles the ghostly transparency. */
    private static final int COLOR_RGB = 0xFF5555;

    /** Scale factor; larger = more legible at range. */
    private static final float ICON_SIZE = 1.4f;

    /** Opacity (0..1). */
    private static final float ICON_ALPHA = 0.75f;

    /** World-space offset above the block so the icon hovers over the former spawn. */
    private static final double Y_OFFSET = 1.2;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().destroyedSpawnMarker) return;
        if (ctx.level() == null) return;

        BlockPos pos;
        ResourceKey<Level> dim;
        if (HandySpawn.session().isServerSynced()) {
            var rec = HandySpawn.personal().destroyedAt().orElse(null);
            if (rec == null) return;
            pos = rec.pos();
            dim = rec.dimension();
        } else {
            var rec = HandySpawn.compassTarget().state().destroyedAt().orElse(null);
            if (rec == null) return;
            pos = rec.pos();
            dim = rec.dimension();
        }
        if (!dim.equals(ctx.level().dimension())) return;

        Vec3 above = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 + Y_OFFSET, pos.getZ() + 0.5);
        // Always through-walls for ghost marker so it's findable even with blocks built on top.
        RenderPrimitives.billboardGlyph(ctx, above, GLYPH, ICON_SIZE, COLOR_RGB, ICON_ALPHA, true);
    }

    private GhostMarkerRenderer() {}
}
