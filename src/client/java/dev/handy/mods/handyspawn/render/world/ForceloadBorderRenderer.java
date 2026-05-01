package dev.handy.mods.handyspawn.render.world;

import dev.handy.mods.handyspawn.HandySpawnClient;
import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;

/**
 * F9 — draws translucent orange walls around every force-loaded chunk.
 *
 * <p>Each chunk renders as four full-height vertical quads (minY → maxY),
 * giving the same footprint as the old wireframe beams but as filled, mostly
 * transparent faces. Adjacent force-loaded chunks' walls overlap along
 * shared edges — that doubled tint is intentional and reads as a brighter
 * seam along the chunk grid.
 *
 * <p>Data comes from {@link dev.handy.mods.handyspawn.state.SharedForceloadState}, which
 * is populated by {@link dev.handy.mods.handyspawn.net.ForceloadSyncPayload} broadcasts
 * from the server. Dimensions the server has not sent an update for simply
 * resolve to an empty set, so {@link #render} becomes a no-op rather than
 * needing an explicit availability check.
 *
 * <p>Opt-in via {@code HandySpawnConfig.forceloadBorderOverlay} — off by default
 * because this is a redstone-builder convenience, not something most players want
 * permanently on.
 */
public final class ForceloadBorderRenderer {

    // Two tint schemes so the player reads "I am / am not inside a loaded
    // chunk" from the colour of the wall in front of them, independent of the
    // HUD badge. Both use translucent fills with a brighter, higher-alpha
    // outline colour for the perimeter edges.
    private static final int OUTSIDE_FILL = 0x30FF9030; // orange
    private static final int OUTSIDE_EDGE = 0xE0FF9030;
    private static final int INSIDE_FILL  = 0x3040E0FF; // cyan
    private static final int INSIDE_EDGE  = 0xE040E0FF;

    public static void render(RenderContext ctx) {
        if (!HandySpawnConfig.get().forceloadBorderOverlay) return;
        if (ctx.level() == null) return;

        LongSet chunks = HandySpawnClient.forceload().forceloadedChunks(ctx.level().dimension());
        if (chunks.isEmpty()) return;

        // "Inside" means the camera's chunk is itself force-loaded. Using
        // camera (not player) position so the colour tracks F5 third-person
        // and spectator view as the user expects.
        int camCx = (int) Math.floor(ctx.camera().position().x) >> 4;
        int camCz = (int) Math.floor(ctx.camera().position().z) >> 4;
        boolean inside = chunks.contains(ChunkPos.pack(camCx, camCz));
        int fill = inside ? INSIDE_FILL : OUTSIDE_FILL;
        int edge = inside ? INSIDE_EDGE : OUTSIDE_EDGE;

        LongIterator it = chunks.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            int cx = ChunkPos.getX(packed);
            int cz = ChunkPos.getZ(packed);
            // Suppress a wall where the adjacent chunk is also loaded, so
            // contiguous regions render as one continuous hollow column.
            boolean drawNorth = !chunks.contains(ChunkPos.pack(cx,     cz - 1));
            boolean drawSouth = !chunks.contains(ChunkPos.pack(cx,     cz + 1));
            boolean drawWest  = !chunks.contains(ChunkPos.pack(cx - 1, cz    ));
            boolean drawEast  = !chunks.contains(ChunkPos.pack(cx + 1, cz    ));
            RenderPrimitives.forceLoadSurface(ctx, cx, cz, fill, edge,
                drawNorth, drawSouth, drawWest, drawEast);
        }
    }

    private ForceloadBorderRenderer() {}
}
