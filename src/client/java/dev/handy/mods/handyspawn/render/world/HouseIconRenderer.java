package dev.handy.mods.handyspawn.render.world;

import dev.handy.mods.handyspawn.HandySpawn;
import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * F1 feature: billboard a small house sprite above the player's current spawn
 * (bed or charged respawn anchor) while in the same dimension. When the server
 * has HandySpawn installed the marker tracks the personal per-player spawn
 * (via {@link dev.handy.mods.handyspawn.state.PersonalSpawnState}); otherwise it falls
 * back to the compass target / world spawn synced through vanilla channels.
 * Cross-dimension callout is handled by the HUD feature (F6).
 */
public final class HouseIconRenderer {

    /**
     * Plain ASCII label — used in lieu of a sprite because {@link
     * RenderPrimitives#billboardGlyph} is the working billboard path in
     * MC 26.1.2 under our injection point. The vanilla default font does
     * not ship U+2302 (HOUSE) glyph coverage, so we use a readable word.
     * A sprite-based path is tracked as a polish task.
     */
    private static final String GLYPH = "HOME";

    /** Warm off-white, reads as "alive / active" next to the ghost marker's red. */
    private static final int COLOR_RGB = 0xFFF0C8;

    /** Scale factor; larger = more legible at range. */
    private static final float ICON_SIZE = 1.4f;

    /** Opacity (0..1). */
    private static final float ICON_ALPHA = 0.95f;

    /** World-space offset above the block so the icon hovers over the bed/anchor. */
    private static final double Y_OFFSET = 1.2;

    public static void render(RenderContext ctx) {
        HandySpawnConfig cfg = HandySpawnConfig.get();
        if (!cfg.personalSpawnMarker) return;
        if (ctx.level() == null) return;

        BlockPos pos;
        ResourceKey<Level> dim;
        if (HandySpawn.session().isServerSynced()) {
            var record = HandySpawn.personal().current().orElse(null);
            if (record == null) return;
            pos = record.pos();
            dim = record.dimension();
        } else {
            var record = HandySpawn.compassTarget().state().current().orElse(null);
            if (record == null) return;
            pos = record.pos();
            dim = record.dimension();
        }
        if (!dim.equals(ctx.level().dimension())) return;

        Vec3 above = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 + Y_OFFSET, pos.getZ() + 0.5);
        RenderPrimitives.billboardGlyph(ctx, above, GLYPH, ICON_SIZE, COLOR_RGB, ICON_ALPHA, false);
    }

    private HouseIconRenderer() {}
}
