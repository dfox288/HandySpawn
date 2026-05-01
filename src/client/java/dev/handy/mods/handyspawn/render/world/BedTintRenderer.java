package dev.handy.mods.handyspawn.render.world;

import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * F2 feature: float a small "Zzz" billboard above every bed within
 * {@link #SCAN_RADIUS} blocks of the player, but only when sleep would actually
 * succeed right now — applies to ANY bed, not just the player's spawn, so it
 * also works as a "which beds are usable" hint in shared bases.
 *
 * <p>Sleep conditions (MC 26.1.2):
 * <ul>
 *   <li>Night — {@link Level#isDarkOutside()}</li>
 *   <li>Active thunderstorm — {@link Level#isThundering()}</li>
 * </ul>
 * Plain rain does not qualify.
 *
 * <p>We render once per bed (at the HEAD half only) so bunk beds placed side
 * by side don't stack two Zzz markers at the same world point. The marker is
 * positioned at the geometric midpoint between HEAD and FOOT so the billboard
 * hovers over the bed rather than one half.
 *
 * <p>Per-frame iteration over ~17k positions (32×32×9) is tolerable for a
 * single player. If profiling shows cost, a cached scan that refreshes every
 * ~20 ticks would be the next step.
 */
public final class BedTintRenderer {

    private static final String GLYPH = "Zzz";
    /** Soft green, matches the prior tint colour (0x40FF40 base). */
    private static final int COLOR_RGB = 0x80FF90;
    private static final float ICON_SIZE = 0.9f;
    /** Gentle alpha pulse — low / high bounds and cycle period. */
    private static final float PULSE_MIN_ALPHA = 0.55f;
    private static final float PULSE_MAX_ALPHA = 1.00f;
    private static final long  PULSE_PERIOD_MS = 1800L;

    /** World-space offset above the HEAD block's origin. The mattress top is
     * at {@code y + 9/16}; this hovers the glyph ~7/16 above that. */
    private static final double Y_OFFSET = 1.0;

    /** Horizontal scan radius in blocks from the player. */
    private static final int SCAN_RADIUS = 16;

    /** Vertical scan radius in blocks from the player (beds on nearby floors). */
    private static final int VERTICAL_RADIUS = 4;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().sleepReadinessTint) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ctx.level() == null) return;
        Level w = ctx.level();

        boolean sleepable = w.isDarkOutside() || w.isThundering();
        if (!sleepable) return;

        // Pulse once per frame so every bed breathes in sync.
        double phase = (System.currentTimeMillis() % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float pulse = 0.5f + 0.5f * (float) Math.sin(phase * 2 * Math.PI);
        float alpha = PULSE_MIN_ALPHA + (PULSE_MAX_ALPHA - PULSE_MIN_ALPHA) * pulse;

        BlockPos center = mc.player.blockPosition();
        BlockPos from = center.offset(-SCAN_RADIUS, -VERTICAL_RADIUS, -SCAN_RADIUS);
        BlockPos to = center.offset(SCAN_RADIUS, VERTICAL_RADIUS, SCAN_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = w.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)) continue;
            // Render once per bed: only when we hit the HEAD half. FOOT half
            // is skipped so we don't double-draw the same glyph.
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) continue;

            // getConnectedDirection(HEAD) returns the direction from HEAD to
            // its FOOT partner, so adding half that step vector gives the
            // midpoint between the two halves.
            Direction toFoot = BedBlock.getConnectedDirection(state);
            Vec3 bedCentre = new Vec3(
                pos.getX() + 0.5 + toFoot.getStepX() * 0.5,
                pos.getY() + Y_OFFSET,
                pos.getZ() + 0.5 + toFoot.getStepZ() * 0.5);

            RenderPrimitives.billboardGlyph(ctx, bedCentre, GLYPH, ICON_SIZE, COLOR_RGB, alpha, false);
        }
    }

    private BedTintRenderer() {}
}
