package dev.handy.mods.handyspawn.state;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of the server's per-dimension forceload set, populated
 * from {@link dev.handy.mods.handyspawn.net.ForceloadSyncPayload} broadcasts. Replaces
 * the old {@code ForceloadState} that reached into the integrated server —
 * this variant works on dedicated servers too, provided the server has
 * HandySpawn installed.
 *
 * <p>Dimensions with no forceloaded chunks are dropped from the map so
 * callers iterating by dimension don't have to special-case empty sets.
 */
public final class SharedForceloadState {

    private final Map<ResourceKey<Level>, LongSet> byDim = new ConcurrentHashMap<>();

    public void update(ResourceKey<Level> dim, long[] packedChunks) {
        if (packedChunks.length == 0) {
            byDim.remove(dim);
            return;
        }
        byDim.put(dim, new LongOpenHashSet(packedChunks));
    }

    public LongSet forceloadedChunks(ResourceKey<Level> dim) {
        LongSet set = byDim.get(dim);
        return set != null ? set : LongSet.of();
    }

    public boolean containsChunk(ResourceKey<Level> dim, ChunkPos pos) {
        return forceloadedChunks(dim).contains(pos.pack());
    }

    public void clear() {
        byDim.clear();
    }
}
