package dev.handyspawn.render.world;

import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;

/**
 * F2 feature: paint a translucent green overlay on the top face of every bed
 * within {@link #SCAN_RADIUS} blocks of the player, but only when sleep would
 * actually succeed right now.
 *
 * <p>Sleep conditions (MC 26.1.2):
 * <ul>
 *   <li>Night — {@link Level#isDarkOutside()} (Mojang renamed the old
 *       {@code getDayTime()} / in-window check to this composite predicate
 *       during the 26.1 cycle; it accounts for dimension fixed-time etc.)</li>
 *   <li>Active thunderstorm — {@link Level#isThundering()}</li>
 * </ul>
 * Plain rain does not qualify.
 *
 * <p>Per-frame iteration over ~17k positions (32×32×9) is tolerable for a
 * single player. If profiling shows cost, a cached scan that refreshes every
 * ~20 ticks would be the next step.
 */
public final class BedTintRenderer {

    /** Semi-transparent green (ARGB). */
    private static final int TINT_ARGB = 0x6040FF40;

    /** Horizontal scan radius in blocks from the player. */
    private static final int SCAN_RADIUS = 16;

    /** Vertical scan radius in blocks from the player (beds on nearby floors). */
    private static final int VERTICAL_RADIUS = 4;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().sleepReadinessTint) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || ctx.level() == null) return;
        Level w = ctx.level();

        boolean sleepable = w.isDarkOutside() || w.isThundering();
        if (!sleepable) return;

        BlockPos center = mc.player.blockPosition();
        BlockPos from = center.offset(-SCAN_RADIUS, -VERTICAL_RADIUS, -SCAN_RADIUS);
        BlockPos to = center.offset(SCAN_RADIUS, VERTICAL_RADIUS, SCAN_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (!(w.getBlockState(pos).getBlock() instanceof BedBlock)) continue;
            RenderPrimitives.topFaceTint(ctx, pos, TINT_ARGB);
        }
    }

    private BedTintRenderer() {}
}
