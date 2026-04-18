package dev.handyspawn;

import dev.handyspawn.net.ForceloadSyncPayload;
import dev.handyspawn.net.HandshakePayload;
import dev.handyspawn.net.HandySpawnPayloads;
import dev.handyspawn.net.PersonalSpawnKind;
import dev.handyspawn.server.SpawnBroadcaster;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
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
        HandySpawnConstants.LOG.info("HandySpawn server component initialising");
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

        // When a bed or respawn anchor is destroyed, any player whose personal
        // spawn was bound to exactly that position needs to be notified and
        // have their server-side respawn cleared. Otherwise vanilla will still
        // try to respawn them at the missing block on next death, and the
        // client-side marker overlay will lag behind reality.
        //
        // Bed subtlety: vanilla records a player's spawn at the HEAD of the
        // bed, but a player can break either half. Breaking the HEAD gives us
        // a direct pos-equals check. Breaking the FOOT does not — we have to
        // resolve the FOOT's partner (= HEAD) from the state's FACING, and
        // compare THAT to each player's spawn pos. Post-break world-state
        // checks were unreliable in practice (the HEAD's updateShape-driven
        // removal is synchronous in setBlock, but any mod or later hook that
        // swaps the block back — or a FOOT without a resolvable HEAD neighbor
        // — defeats it). Comparing against both the broken pos and its
        // computed partner pos is robust by construction.
        PlayerBlockBreakEvents.AFTER.register((world, breaker, pos, state, blockEntity) -> {
            boolean isBed = state.getBlock() instanceof BedBlock;
            boolean isAnchor = state.getBlock() instanceof RespawnAnchorBlock;
            if (!isBed && !isAnchor) return;
            if (!(world instanceof ServerLevel serverLevel)) return;

            HandySpawnConstants.LOG.info(
                "[break] block={} pos={} bed={} anchor={} breaker={}",
                state.getBlock().getClass().getSimpleName(), pos, isBed, isAnchor,
                breaker == null ? "null" : breaker.getName().getString());

            ResourceKey<Level> worldKey = serverLevel.dimension();

            // Pre-compute all positions a player's spawnPos could match for
            // this break event. For a bed: the broken pos plus the partner
            // pos (derived from the state's PART + FACING). For an anchor:
            // just the broken pos.
            BlockPos partnerPos = isBed ? bedPartnerPos(pos, state) : null;

            for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                ServerPlayer.RespawnConfig rc = p.getRespawnConfig();
                if (rc == null) continue;
                LevelData.RespawnData data = rc.respawnData();
                if (data == null) continue;
                BlockPos spawnPos = data.pos();
                ResourceKey<Level> spawnDim = data.dimension();
                if (!worldKey.equals(spawnDim)) continue;

                boolean hit = spawnPos.equals(pos)
                           || (partnerPos != null && spawnPos.equals(partnerPos));
                HandySpawnConstants.LOG.info(
                    "[break] player={} spawnPos={} brokenPos={} partnerPos={} hit={}",
                    p.getName().getString(), spawnPos, pos, partnerPos, hit);
                if (!hit) continue;

                PersonalSpawnKind kind = isAnchor
                    ? PersonalSpawnKind.RESPAWN_ANCHOR
                    : PersonalSpawnKind.BED;
                SpawnBroadcaster.broadcastDestroyed(p, spawnPos, spawnDim, kind);
                // Pass null to clear; vanilla's setRespawnPosition treats a
                // null config as a clear (and the false flag suppresses the
                // "spawn point set" chat message).
                p.setRespawnPosition(null, false);
            }
        });
    }

    /**
     * Given a broken bed's pos + state, returns the pos of the OTHER half of
     * the bed (HEAD if {@code state} is the FOOT half, FOOT if HEAD). Uses the
     * block state's own FACING/PART values so we don't have to query the world
     * — this is essential since our caller runs after the break and the
     * neighbor half may already be gone. Returns null if the state isn't a
     * bed state (defensive; caller already checks for BedBlock).
     */
    private static BlockPos bedPartnerPos(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock)) return null;
        // BedBlock.getConnectedDirection(state) returns the direction pointing
        // from the current block TO its other half, irrespective of which
        // half the current block is (it inverts FACING for the HEAD half so
        // the caller doesn't have to care).
        Direction toPartner = BedBlock.getConnectedDirection(state);
        return pos.relative(toPartner);
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
