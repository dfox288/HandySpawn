package dev.handy.mods.handyspawn;

import dev.handy.mods.handyspawn.net.ForceloadSyncPayload;
import dev.handy.mods.handyspawn.net.HandshakePayload;
import dev.handy.mods.handyspawn.net.HandySpawnPayloads;
import dev.handy.mods.handyspawn.spawn.SpawnBlockKind;
import dev.handy.mods.handyspawn.server.SpawnBroadcaster;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.storage.LevelData;

/**
 * Server-side entrypoint for HandySpawn.
 * <p>
 * Registers the mod's custom payload types on the clientbound play channel and
 * sends a {@link HandshakePayload} to every player as soon as they finish
 * joining. The handshake's sole purpose is presence-detection: receiving it
 * tells the client that the server speaks HandySpawn, so the client can switch
 * from its singleplayer-only fallback path to server-synced state. Immediately
 * after the handshake we also push the player's current personal spawn via
 * {@link SpawnBroadcaster#broadcastPersonalSpawn(ServerPlayer)}, so reconnecting
 * players don't have to wait for their next bed interaction before the client
 * marker is restored.
 */
public final class HandySpawnServer implements ModInitializer {

    @Override
    public void onInitialize() {
        HandySpawnConstants.LOGGER.info("HandySpawn server component initialising");
        HandySpawnPayloads.registerAll();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            ServerPlayNetworking.send(player, HandshakePayload.INSTANCE);
            SpawnBroadcaster.broadcastPersonalSpawn(player);

            // Seed the fresh client with per-dimension forceload snapshots so
            // markers are accurate before the first mutation arrives. Empty
            // sets are skipped - the client treats an absent entry as "none".
            for (ServerLevel lvl : server.getAllLevels()) {
                long[] arr = lvl.getForceLoadedChunks().toLongArray();
                if (arr.length == 0) continue;
                ServerPlayNetworking.send(player, new ForceloadSyncPayload(lvl.dimension(), arr));
            }
        });

        // Respawn-anchor destruction detection. Bed destruction is handled
        // separately by BedBlockMixin — Fabric's PlayerBlockBreakEvents.AFTER
        // turned out to be unreliable for bed FOOT-half breaks in MC 26.1.2
        // (a FOOT click in survival did not dispatch the event), so we hook
        // BedBlock.playerWillDestroy directly. Anchors are a single block and
        // the event fires reliably for them, so we keep this lightweight path.
        PlayerBlockBreakEvents.AFTER.register((world, breaker, pos, state, blockEntity) -> {
            if (!(state.getBlock() instanceof RespawnAnchorBlock)) return;
            if (!(world instanceof ServerLevel serverLevel)) return;

            ResourceKey<Level> worldKey = serverLevel.dimension();
            for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                ServerPlayer.RespawnConfig rc = p.getRespawnConfig();
                if (rc == null) continue;
                LevelData.RespawnData data = rc.respawnData();
                if (data == null) continue;
                BlockPos spawnPos = data.pos();
                ResourceKey<Level> spawnDim = data.dimension();
                if (!worldKey.equals(spawnDim)) continue;
                if (!spawnPos.equals(pos)) continue;

                SpawnBroadcaster.broadcastDestroyed(p, spawnPos, spawnDim, SpawnBlockKind.RESPAWN_ANCHOR);
                p.setRespawnPosition(null, false);
            }
        });
    }

    /**
     * Broadcasts the current full force-loaded chunk set for {@code level} to
     * every player currently in that dimension.
     * <p>
     * Called from {@code ServerLevelForceloadMixin} at the tail of
     * {@code setChunkForced}, so the snapshot reflects the post-mutation
     * state. Sending full snapshots (rather than per-chunk deltas) keeps the
     * client state trivially correct after any reorder or dropped packet and
     * costs at most {@code 8 * N} bytes per change, which is negligible for
     * realistic forceload set sizes.
     */
    public static void onForceloadChanged(ServerLevel level) {
        LongSet chunks = level.getForceLoadedChunks();
        long[] arr = chunks.toLongArray();
        ForceloadSyncPayload payload = new ForceloadSyncPayload(level.dimension(), arr);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
