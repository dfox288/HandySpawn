package dev.handyspawn;

import dev.handyspawn.net.HandshakePayload;
import dev.handyspawn.net.HandySpawnPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Server-side entrypoint for HandySpawn.
 * <p>
 * Registers the mod's custom payload types on the clientbound play channel and
 * sends a {@link HandshakePayload} to every player as soon as they finish
 * joining. The handshake's sole purpose is presence-detection: receiving it
 * tells the client that the server speaks HandySpawn, so the client can switch
 * from its singleplayer-only fallback path to server-synced state.
 */
public final class HandySpawnServer implements ModInitializer {

    @Override
    public void onInitialize() {
        HandySpawnConstants.LOG.info("HandySpawn server component initialising");
        HandySpawnPayloads.registerAll();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            ServerPlayNetworking.send(handler.player, HandshakePayload.INSTANCE));
    }
}
