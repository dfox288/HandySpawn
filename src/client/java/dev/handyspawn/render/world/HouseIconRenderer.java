package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * F1 feature: billboard a small house sprite above the player's current spawn
 * (bed or charged respawn anchor) while in the same dimension. When the server
 * has HandySpawn installed the marker tracks the personal per-player spawn
 * (via {@link dev.handyspawn.state.PersonalSpawnState}); otherwise it falls
 * back to the compass target / world spawn synced through vanilla channels.
 * Cross-dimension callout is handled by the HUD feature (F6).
 */
public final class HouseIconRenderer {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/house.png");

    /** Quad side length in world units. */
    private static final float ICON_SIZE = 0.6f;

    /** Opacity (0..1). */
    private static final float ICON_ALPHA = 0.9f;

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
        RenderPrimitives.billboardIcon(ctx, above, SPRITE, ICON_SIZE, ICON_ALPHA,
            cfg.personalSpawnThroughWalls);
    }

    private HouseIconRenderer() {}
}
