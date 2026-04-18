package dev.handyspawn.state;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ForceloadState {

    public boolean isAvailable() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getSingleplayerServer() != null;
    }

    public LongSet forceloadedChunks(ResourceKey<Level> dimension) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return LongSet.of();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return LongSet.of();
        ServerLevel world = server.getLevel(dimension);
        if (world == null) return LongSet.of();
        return new LongOpenHashSet(world.getForceLoadedChunks());
    }

    public boolean containsChunk(ResourceKey<Level> dim, ChunkPos pos) {
        return forceloadedChunks(dim).contains(pos.pack());
    }
}
