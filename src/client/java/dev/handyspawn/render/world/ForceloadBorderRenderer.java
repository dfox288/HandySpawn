package dev.handyspawn.render.world;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.config.HandySpawnConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;

/**
 * F9 — draws an orange chunk-border wireframe around every chunk currently
 * marked via {@code /forceload add} in the active dimension.
 *
 * <p>Singleplayer / integrated-server only: {@link dev.handyspawn.state.ForceloadState}
 * reaches into the in-process {@code IntegratedServer} to read the authoritative
 * forceload set. On dedicated servers it returns empty and {@link #render} is a no-op.
 *
 * <p>Opt-in via {@code HandySpawnConfig.forceloadBorderOverlay} — off by default
 * because this is a redstone-builder convenience, not something most players want
 * permanently on.
 */
public final class ForceloadBorderRenderer {

    /** Opaque orange — matches the F9 spec colour. */
    private static final int COLOUR_ARGB = 0xFFFF9030;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().forceloadBorderOverlay) return;
        if (ctx.level() == null) return;
        if (!HandySpawn.forceload().isAvailable()) return;

        LongSet chunks = HandySpawn.forceload().forceloadedChunks(ctx.level().dimension());
        if (chunks.isEmpty()) return;

        LongIterator it = chunks.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            RenderPrimitives.chunkWireframe(ctx, ChunkPos.getX(packed), ChunkPos.getZ(packed), COLOUR_ARGB);
        }
    }

    private ForceloadBorderRenderer() {}
}
