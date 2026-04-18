package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.SpawnRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * F3 feature: billboard a red ghost-house sprite at the coordinates where the
 * player's former personal spawn used to be (destroyed bed / broken respawn
 * anchor). Always rendered through walls — a destroyed spawn with something
 * built on top is a core use case for this marker.
 */
public final class GhostMarkerRenderer {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/house_ghost.png");

    /** Quad side length in world units. */
    private static final float ICON_SIZE = 0.6f;

    /** Opacity (0..1). */
    private static final float ICON_ALPHA = 0.7f;

    /** World-space offset above the block so the icon hovers over the former spawn. */
    private static final double Y_OFFSET = 1.2;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().destroyedSpawnMarker) return;
        if (ctx.level() == null) return;

        SpawnRecord rec = HandySpawn.spawn().state().destroyedAt().orElse(null);
        if (rec == null) return;
        if (!rec.dimension().equals(ctx.level().dimension())) return;

        BlockPos pos = rec.pos();
        Vec3 above = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5 + Y_OFFSET, pos.getZ() + 0.5);
        // Always through-walls for ghost marker so it's findable even with blocks built on top.
        RenderPrimitives.billboardIcon(ctx, above, SPRITE, ICON_SIZE, ICON_ALPHA, true);
    }

    private GhostMarkerRenderer() {}
}
