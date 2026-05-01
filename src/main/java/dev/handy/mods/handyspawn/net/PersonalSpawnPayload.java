package dev.handy.mods.handyspawn.net;

import dev.handy.mods.handyspawn.HandySpawnConstants;
import dev.handy.mods.handyspawn.spawn.SpawnBlockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * S2C payload carrying a player's personal spawn state.
 * <p>
 * Semantics:
 * <ul>
 *   <li>{@code isDestroyed == true} — the bed or anchor at {@code pos}/{@code dimension}
 *       was just removed. {@code pos} and {@code dimension} carry the OLD location.</li>
 *   <li>{@code isDestroyed == false} and {@code pos.isPresent()} — that's the player's
 *       current personal spawn.</li>
 *   <li>{@code isDestroyed == false} and {@code pos.isEmpty()} — the player currently
 *       has no personal spawn.</li>
 * </ul>
 */
public record PersonalSpawnPayload(
    Optional<BlockPos> pos,
    Optional<ResourceKey<Level>> dimension,
    SpawnBlockKind kind,
    boolean isDestroyed
) implements CustomPacketPayload {

    public static final Type<PersonalSpawnPayload> ID =
        new Type<>(Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "personal_spawn"));

    private static final StreamCodec<io.netty.buffer.ByteBuf, SpawnBlockKind> KIND_CODEC =
        StreamCodec.of(
            (buf, v) -> buf.writeByte(v.ordinal()),
            buf -> SpawnBlockKind.values()[buf.readByte()]
        );

    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalSpawnPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PersonalSpawnPayload::pos,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), PersonalSpawnPayload::dimension,
            KIND_CODEC, PersonalSpawnPayload::kind,
            ByteBufCodecs.BOOL, PersonalSpawnPayload::isDestroyed,
            PersonalSpawnPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
