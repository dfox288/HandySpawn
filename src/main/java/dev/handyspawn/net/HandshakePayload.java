package dev.handyspawn.net;

import dev.handyspawn.HandySpawnConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Empty, singleton S2C handshake payload. Sent by the server when a player joins
 * so the client knows the server speaks HandySpawn and can start accepting other
 * HandySpawn payloads.
 */
public record HandshakePayload() implements CustomPacketPayload {

    public static final HandshakePayload INSTANCE = new HandshakePayload();

    public static final Type<HandshakePayload> ID =
        new Type<>(Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "handshake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
