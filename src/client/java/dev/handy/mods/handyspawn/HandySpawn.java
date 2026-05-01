package dev.handy.mods.handyspawn;

import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import dev.handy.mods.handyspawn.net.ForceloadSyncPayload;
import dev.handy.mods.handyspawn.net.HandshakePayload;
import dev.handy.mods.handyspawn.net.HandySpawnPayloads;
import dev.handy.mods.handyspawn.net.PersonalSpawnPayload;
import dev.handy.mods.handyspawn.state.CompassTargetStateController;
import dev.handy.mods.handyspawn.state.PersonalSpawnState;
import dev.handy.mods.handyspawn.state.PhantomTimer;
import dev.handy.mods.handyspawn.state.SessionState;
import dev.handy.mods.handyspawn.state.SharedForceloadState;
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
    private static SessionState sessionState;

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

    /**
     * Per-connection session / handshake state. Renderers read
     * {@link SessionState#isServerSynced()} to choose between
     * {@link PersonalSpawnState} (server-synced) and the compass-target
     * fallback.
     */
    public static SessionState session() {
        return sessionState;
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOGGER.info("HandySpawn initialising");
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
        sessionState = new SessionState();

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, ctx) -> {
            sessionState.onHandshakeReceived();
            HandySpawnConstants.LOGGER.info("Server has HandySpawn installed");
        });

        ClientPlayNetworking.registerGlobalReceiver(PersonalSpawnPayload.ID, (payload, ctx) -> {
            personalSpawnState.applyPayload(
                payload.pos(), payload.dimension(), payload.kind(), payload.isDestroyed());
        });

        ClientPlayNetworking.registerGlobalReceiver(ForceloadSyncPayload.ID, (payload, ctx) -> {
            forceloadState.update(payload.dimension(), payload.packedChunks());
        });

        // Mark the session as started on join; CompassTargetStateController
        // registers its own JOIN handler to load the persisted compass target.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sessionState.onJoin();
        });

        // Clear transient per-connection state on disconnect so a later join
        // doesn't inherit the previous server's spawns or forceloaded chunks.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            sessionState.onDisconnect();
            personalSpawnState.clear();
            forceloadState.clear();
        });

        phantomTimer = PhantomTimer.create();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            sessionState.onTick(msg -> HandySpawnConstants.LOGGER.info(msg));
            if (client.player == null || client.getConnection() == null) return;
            if (phantomTimer.shouldRequestStatsThisTick(STATS_POLL_INTERVAL_TICKS)) {
                client.getConnection().send(
                    new ServerboundClientCommandPacket(
                        ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            int value = client.player.getStats().getValue(Stats.CUSTOM, Stats.TIME_SINCE_REST);
            phantomTimer.updateFromStats(value);
        });

        // World-space rendering uses Fabric's LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN
        // so our vertices land inside vanilla's live render pass (PoseStack with
        // view matrix applied, BufferSource accepting vertices, batches not yet ended).
        dev.handy.mods.handyspawn.render.world.WorldRenderer.register();
        dev.handy.mods.handyspawn.render.hud.HudRenderer.register();
    }
}
