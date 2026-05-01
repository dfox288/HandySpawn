package dev.handy.mods.handyspawn.server;

import dev.handy.mods.handyspawn.net.PersonalSpawnPayload;
import dev.handy.mods.handyspawn.spawn.SpawnBlockKind;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;

/**
 * Server-side helper that pushes the current personal-spawn state of a given
 * player over the {@link PersonalSpawnPayload} channel.
 * <p>
 * Called from two places:
 * <ul>
 *   <li>the {@code ServerPlayer.setRespawnPosition} mixin, right after vanilla
 *       mutates the player's respawn config;</li>
 *   <li>the {@code JOIN} connection event, so a client that reconnects with a
 *       respawn already on file gets the correct marker without waiting for
 *       the next bed interaction.</li>
 * </ul>
 * The broadcaster inspects the block at the stored position to tag the payload
 * as {@link SpawnBlockKind#BED} or {@link SpawnBlockKind#RESPAWN_ANCHOR};
 * if the block has been replaced since the spawn was set, it falls back to
 * {@code BED}.
 */
public final class SpawnBroadcaster {

    private SpawnBroadcaster() {}

    /**
     * Sends the player's current personal-spawn state. If the player has no
     * respawn configured, an empty payload is sent so the client can clear any
     * stale marker from a previous session or server.
     */
    public static void broadcastPersonalSpawn(ServerPlayer player) {
        ServerPlayer.RespawnConfig rc = player.getRespawnConfig();
        if (rc == null) {
            ServerPlayNetworking.send(player, new PersonalSpawnPayload(
                Optional.empty(),
                Optional.empty(),
                SpawnBlockKind.BED,
                false
            ));
            return;
        }
        LevelData.RespawnData data = rc.respawnData();
        BlockPos pos = data.pos();
        ResourceKey<Level> dim = data.dimension();
        SpawnBlockKind kind = inferKind(player.level().getServer(), pos, dim);
        ServerPlayNetworking.send(player, new PersonalSpawnPayload(
            Optional.of(pos),
            Optional.of(dim),
            kind,
            false
        ));
    }

    /**
     * Notifies the client that a spawn-bearing block was just destroyed. The
     * old location is included so the client can play a "poof" animation or
     * otherwise reconcile its state before the next authoritative payload.
     */
    public static void broadcastDestroyed(ServerPlayer player,
                                          BlockPos oldPos,
                                          ResourceKey<Level> oldDim,
                                          SpawnBlockKind oldKind) {
        ServerPlayNetworking.send(player, new PersonalSpawnPayload(
            Optional.of(oldPos),
            Optional.of(oldDim),
            oldKind,
            true
        ));
    }

    private static SpawnBlockKind inferKind(MinecraftServer server,
                                               BlockPos pos,
                                               ResourceKey<Level> dim) {
        if (server != null) {
            ServerLevel world = server.getLevel(dim);
            if (world != null) {
                var block = world.getBlockState(pos).getBlock();
                if (block instanceof RespawnAnchorBlock) {
                    return SpawnBlockKind.RESPAWN_ANCHOR;
                }
                if (block instanceof BedBlock) {
                    return SpawnBlockKind.BED;
                }
            }
        }
        return SpawnBlockKind.BED;
    }
}
