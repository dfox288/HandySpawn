package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.CompassTargetRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * F7 feature: billboard a subtle compass glyph at the world's default spawn
 * block, overworld only.
 *
 * <p>API note (MC 26.1.2): the world spawn is plumbed to the client via
 * {@link net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket},
 * sent by {@code MinecraftServer.setRespawnData} on login and whenever
 * {@code /setworldspawn} runs. {@code CompassTargetStateController} is the single
 * source of truth — reading through it keeps F1 (house icon) and F7 (this
 * glyph) consistent and lets F7 also react to destroyed-compass-target
 * verification.
 *
 * <p>Note: personal bed / respawn-anchor spawns stored server-side on
 * {@code ServerPlayer.respawnConfig} are not synced to the client in 26.1.2,
 * so F7 and F1 necessarily show the same coordinates — they are rendered
 * distinctly via sprite, size and opacity.
 *
 * <p>Rendered through-walls = false to match the subtle aesthetic.
 */
public final class WorldSpawnRenderer {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/worldspawn.png");

    /** Quad side length in world units. Slightly larger than the house icon to read as a landmark. */
    private static final float ICON_SIZE = 0.8f;

    /** Opacity (0..1) — subtle. */
    private static final float ICON_ALPHA = 0.8f;

    /** World-space offset above the spawn block so the glyph hovers. */
    private static final double Y_OFFSET = 1.5;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().worldSpawnMarker) return;
        if (ctx.level() == null) return;
        if (!ctx.level().dimension().equals(Level.OVERWORLD)) return;

        CompassTargetRecord rec = HandySpawn.compassTarget().state().current().orElse(null);
        if (rec == null) return;
        if (!rec.dimension().equals(Level.OVERWORLD)) return;

        BlockPos spawn = rec.pos();
        Vec3 above = new Vec3(spawn.getX() + 0.5, spawn.getY() + 0.5 + Y_OFFSET, spawn.getZ() + 0.5);
        RenderPrimitives.billboardIcon(ctx, above, SPRITE, ICON_SIZE, ICON_ALPHA, false);
    }

    private WorldSpawnRenderer() {}
}
