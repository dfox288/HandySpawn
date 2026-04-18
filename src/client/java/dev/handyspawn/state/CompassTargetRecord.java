package dev.handyspawn.state;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Immutable record of a tracked compass-target position (the global compass
 * target / {@code Level.getRespawnData()}).
 *
 * <p>Backed by the data delivered via
 * {@link net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket}
 * and stored on {@code ClientLevel} as {@code LevelData.RespawnData}. The server
 * sends this packet only for the world spawn (on login, and on
 * {@code /setworldspawn}) via {@code MinecraftServer.setRespawnData} /
 * {@code PlayerList.broadcastAll}. Personal per-player bed or respawn-anchor
 * spawns are stored server-side on {@code ServerPlayer.respawnConfig} and are
 * never synced to the client through this channel, so this record represents
 * the compass target only.
 */
public record CompassTargetRecord(BlockPos pos, ResourceKey<Level> dimension, CompassTargetKind kind) {
    public static CompassTargetRecord of(BlockPos pos, ResourceKey<Level> dim, CompassTargetKind kind) {
        return new CompassTargetRecord(pos.immutable(), dim, kind);
    }
}
