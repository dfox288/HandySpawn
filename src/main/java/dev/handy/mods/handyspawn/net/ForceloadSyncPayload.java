package dev.handy.mods.handyspawn.net;

import dev.handy.mods.handyspawn.HandySpawnConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/**
 * S2C payload syncing the full set of force-loaded chunks for one dimension.
 * Chunks are packed as {@code ChunkPos.toLong()} longs.
 */
public record ForceloadSyncPayload(
    ResourceKey<Level> dimension,
    long[] packedChunks
) implements CustomPacketPayload {

    public static final Type<ForceloadSyncPayload> ID =
        new Type<>(Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "forceload_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForceloadSyncPayload> CODEC =
        StreamCodec.of(
            (buf, v) -> {
                buf.writeResourceKey(v.dimension());
                buf.writeVarInt(v.packedChunks().length);
                for (long c : v.packedChunks()) {
                    buf.writeLong(c);
                }
            },
            buf -> {
                ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
                int n = buf.readVarInt();
                long[] arr = new long[n];
                for (int i = 0; i < n; i++) {
                    arr[i] = buf.readLong();
                }
                return new ForceloadSyncPayload(dim, arr);
            }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    // Override the record's default equals/hashCode: `long[]` falls back to identity
    // comparison, so two payloads with the same contents would otherwise compare
    // unequal. Forceload state diffing relies on content equality.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForceloadSyncPayload other)) return false;
        return dimension.equals(other.dimension)
            && Arrays.equals(packedChunks, other.packedChunks);
    }

    @Override
    public int hashCode() {
        return 31 * dimension.hashCode() + Arrays.hashCode(packedChunks);
    }
}
