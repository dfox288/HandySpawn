package dev.handyspawn.state;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;

import java.nio.file.Path;

/**
 * Glue between Minecraft's runtime events and {@link SpawnState} / {@link SpawnPersistence}.
 *
 * <p>On join the controller resolves a stable per-world identifier (singleplayer save name or
 * multiplayer server address), reads any persisted snapshot and restores in-memory state.
 * On disconnect it flushes state to disk. Every client tick it diffs the client level's
 * respawn data against the stored record and persists any changes. When a chunk containing the
 * stored spawn loads, it verifies the underlying block still matches the recorded kind and
 * moves the record to the destroyed slot otherwise.
 */
public final class SpawnStateController {

    private final SpawnState state;
    private final SpawnPersistence persistence;
    private String currentWorldId;

    public SpawnStateController(Path storageDir) {
        this.state = new SpawnState();
        this.persistence = new SpawnPersistence(storageDir);
    }

    public SpawnState state() {
        return state;
    }

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> loadForCurrentWorld(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> flushAndClear());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> verifyDestroyedOnChunkLoad(world, chunk.getPos()));
    }

    private void loadForCurrentWorld(Minecraft client) {
        currentWorldId = resolveWorldId(client);
        SpawnPersistence.Snapshot snap = persistence.read(currentWorldId);
        // SpawnState enforces mutual exclusion: current and destroyed are never both populated.
        if (snap.current().isPresent()) {
            state.setCurrent(snap.current().get());
        } else if (snap.destroyed().isPresent()) {
            state.setCurrent(snap.destroyed().get());
            state.clearCurrent();
        }
    }

    private void flushAndClear() {
        save();
        currentWorldId = null;
    }

    private void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        LevelData.RespawnData respawn = client.level.getRespawnData();
        if (respawn == null || respawn.equals(LevelData.RespawnData.DEFAULT)) {
            if (state.current().isPresent()) {
                state.clearCurrent();
                save();
            }
            return;
        }

        BlockPos spawnPos = respawn.pos();
        ResourceKey<Level> spawnDim = respawn.dimension();
        SpawnKind kind = inferKindFromBlock(client, spawnPos, spawnDim);
        SpawnRecord fresh = SpawnRecord.of(spawnPos, spawnDim, kind);
        if (state.current().map(cur -> !cur.equals(fresh)).orElse(true)) {
            state.setCurrent(fresh);
            save();
        }
    }

    private SpawnKind inferKindFromBlock(Minecraft client, BlockPos pos, ResourceKey<Level> dim) {
        if (client.level != null && client.level.dimension().equals(dim)) {
            var block = client.level.getBlockState(pos).getBlock();
            if (block instanceof RespawnAnchorBlock) return SpawnKind.RESPAWN_ANCHOR;
            if (block instanceof BedBlock) return SpawnKind.BED;
        }
        return state.current().map(SpawnRecord::kind).orElse(SpawnKind.BED);
    }

    private void verifyDestroyedOnChunkLoad(ClientLevel world, ChunkPos cp) {
        state.current().ifPresent(rec -> {
            if (!rec.dimension().equals(world.dimension())) return;
            if (!cp.equals(ChunkPos.containing(rec.pos()))) return;
            var block = world.getBlockState(rec.pos()).getBlock();
            boolean stillValid = switch (rec.kind()) {
                case BED -> block instanceof BedBlock;
                case RESPAWN_ANCHOR -> block instanceof RespawnAnchorBlock;
            };
            if (!stillValid) {
                state.clearCurrent();
                save();
            }
        });
    }

    private void save() {
        if (currentWorldId == null) return;
        persistence.write(currentWorldId, new SpawnPersistence.Snapshot(
            state.current(), state.destroyedAt()));
    }

    private static String resolveWorldId(Minecraft client) {
        IntegratedServer sp = client.getSingleplayerServer();
        if (sp != null) {
            return "sp:" + sp.getWorldData().getLevelName();
        }
        ServerData info = client.getCurrentServer();
        if (info != null) {
            return "mp:" + info.ip;
        }
        return "unknown";
    }
}
