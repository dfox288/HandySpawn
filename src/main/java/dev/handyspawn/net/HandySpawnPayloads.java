package dev.handyspawn.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers HandySpawn's custom network payloads with Fabric's global payload
 * registry. Both server and client initializers call this so that both sides
 * recognise the same payload types on the play channel.
 * <p>
 * All three payloads are S2C (clientbound), so only {@code clientboundPlay()}
 * is registered here. Invoked from the server entrypoint (task V2) and the
 * client entrypoint (task V6); it is safe to call from both because the
 * underlying registry de-duplicates.
 */
public final class HandySpawnPayloads {

    private HandySpawnPayloads() {}

    private static boolean registered;

    /**
     * Registers all three S2C payload types. Idempotent — in singleplayer both
     * the server and client initializers invoke this in the same JVM, so
     * duplicate calls must be no-ops rather than throwing.
     */
    public static synchronized void registerAll() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PersonalSpawnPayload.ID, PersonalSpawnPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ForceloadSyncPayload.ID, ForceloadSyncPayload.CODEC);
        registered = true;
    }
}
