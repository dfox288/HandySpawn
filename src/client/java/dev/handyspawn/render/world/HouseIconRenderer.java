package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.CompassTargetRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * F1 feature: billboard a small house sprite above the tracked world spawn
 * (bed or respawn anchor) while the player is in the same dimension.
 * Cross-dimension callout is handled by the HUD feature (F6).
 *
 * <p>In MC 26.1.2 the server only syncs the world spawn to clients (via
 * {@code ClientboundSetDefaultSpawnPositionPacket}); per-player bed / respawn-anchor
 * spawns live server-side on {@code ServerPlayer.respawnConfig} and are never
 * sent to the client. This marker therefore points at the world spawn rather
 * than an individual player's bed — identical to the F7 marker but with a
 * distinct sprite, size and through-walls option.
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

        CompassTargetRecord rec = HandySpawn.compassTarget().state().current().orElse(null);
        if (rec == null) return;
        if (!rec.dimension().equals(ctx.level().dimension())) return;

        BlockPos pos = rec.pos();
        Vec3 above = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 + Y_OFFSET, pos.getZ() + 0.5);
        RenderPrimitives.billboardIcon(ctx, above, SPRITE, ICON_SIZE, ICON_ALPHA,
            cfg.personalSpawnThroughWalls);
    }

    private HouseIconRenderer() {}
}
