package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

/**
 * F7 feature: billboard a subtle compass glyph at the world's default spawn
 * block, overworld only.
 *
 * <p>API note (MC 26.1.2): Mojang no longer exposes a dedicated
 * {@code Level.getSharedSpawnPos()}. The world-spawn position is plumbed to
 * the client via {@link net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket}
 * and stored into {@code Level} as {@link LevelData.RespawnData}, accessible via
 * {@code Level#getRespawnData()} — the same call used by
 * {@code SpawnStateController} for the personal-spawn lookup. The server sends
 * this packet once on login with the world spawn and again whenever the player's
 * personal spawn changes, so on the client the two coincide: good enough for a
 * subtle glyph, and distinguished from the F1 house icon by the overworld-only
 * filter and the dedicated sprite.
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

        LevelData.RespawnData respawn = ctx.level().getRespawnData();
        if (respawn == null || respawn.equals(LevelData.RespawnData.DEFAULT)) return;

        BlockPos spawn = respawn.pos();
        Vec3 above = new Vec3(spawn.getX() + 0.5, spawn.getY() + 0.5 + Y_OFFSET, spawn.getZ() + 0.5);
        RenderPrimitives.billboardIcon(ctx, above, SPRITE, ICON_SIZE, ICON_ALPHA, false);
    }

    private WorldSpawnRenderer() {}
}
