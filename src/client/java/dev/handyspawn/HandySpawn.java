package dev.handyspawn;

import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.net.ForceloadSyncPayload;
import dev.handyspawn.net.HandshakePayload;
import dev.handyspawn.net.HandySpawnPayloads;
import dev.handyspawn.net.PersonalSpawnPayload;
import dev.handyspawn.state.CompassTargetStateController;
import dev.handyspawn.state.PersonalSpawnState;
import dev.handyspawn.state.PhantomTimer;
import dev.handyspawn.state.SharedForceloadState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;

public final class HandySpawn implements ClientModInitializer {

    /** Poll the server for stats roughly every 10 seconds (200 ticks @ 20tps). */
    private static final int STATS_POLL_INTERVAL_TICKS = 200;

    private static CompassTargetStateController compassTargetController;
    private static PhantomTimer phantomTimer;
    private static PersonalSpawnState personalSpawnState;
    private static SharedForceloadState forceloadState;

    /**
     * Whether the current server sent a {@link HandshakePayload}. This is a
     * temporary gate — V7 replaces it with a richer {@code SessionState} that
     * also tracks connection lifecycle. For now, renderers and HUDs that rely
     * on server-synced data can short-circuit when this is {@code false}.
     */
    private static boolean handshakeReceived;

    /**
     * Access the tracked compass-target state. This is the global compass
     * target / {@code Level.getRespawnData()} (from
     * {@code ClientboundSetDefaultSpawnPositionPacket}), not a per-player bed
     * or respawn-anchor spawn — personal spawns come through
     * {@code PersonalSpawnPayload} separately.
     */
    public static CompassTargetStateController compassTarget() {
        return compassTargetController;
    }

    public static PhantomTimer phantom() {
        return phantomTimer;
    }

    public static SharedForceloadState forceload() {
        return forceloadState;
    }

    public static PersonalSpawnState personal() {
        return personalSpawnState;
    }

    public static boolean serverHasHandyspawn() {
        return handshakeReceived;
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        HandySpawnConfig.load();

        // Register payload types on the client side. The server calls this from
        // its own initializer; the underlying Fabric registry is keyed by payload
        // ID and deduplicates, so running it on both sides is safe and necessary.
        HandySpawnPayloads.registerAll();

        var dir = FabricLoader.getInstance().getConfigDir()
            .resolve(HandySpawnConstants.MOD_ID).resolve("spawns");
        compassTargetController = new CompassTargetStateController(dir);
        compassTargetController.register();

        personalSpawnState = new PersonalSpawnState();
        forceloadState = new SharedForceloadState();

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, ctx) -> {
            handshakeReceived = true;
            HandySpawnConstants.LOG.info("Server has HandySpawn installed");
        });

        ClientPlayNetworking.registerGlobalReceiver(PersonalSpawnPayload.ID, (payload, ctx) -> {
            personalSpawnState.applyPayload(
                payload.pos(), payload.dimension(), payload.kind(), payload.isDestroyed());
        });

        ClientPlayNetworking.registerGlobalReceiver(ForceloadSyncPayload.ID, (payload, ctx) -> {
            forceloadState.update(payload.dimension(), payload.packedChunks());
        });

        // Clear transient per-connection state on disconnect so a later join
        // doesn't inherit the previous server's spawns or forceloaded chunks.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            handshakeReceived = false;
            personalSpawnState.clear();
            forceloadState.clear();
        });

        phantomTimer = PhantomTimer.create();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getConnection() == null) return;
            if (phantomTimer.shouldRequestStatsThisTick(STATS_POLL_INTERVAL_TICKS)) {
                client.getConnection().send(
                    new ServerboundClientCommandPacket(
                        ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            int value = client.player.getStats().getValue(Stats.CUSTOM, Stats.TIME_SINCE_REST);
            phantomTimer.updateFromStats(value);
        });

        // World-space rendering is wired via LevelRendererMixin (see
        // dev.handyspawn.mixin.LevelRendererMixin) — no explicit registration needed here.

        dev.handyspawn.render.hud.HudRenderer.register();
    }
}
