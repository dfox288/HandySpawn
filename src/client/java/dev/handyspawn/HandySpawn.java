package dev.handyspawn;

import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.net.ForceloadSyncPayload;
import dev.handyspawn.net.HandshakePayload;
import dev.handyspawn.net.HandySpawnPayloads;
import dev.handyspawn.net.PersonalSpawnPayload;
import dev.handyspawn.state.CompassTargetStateController;
import dev.handyspawn.state.PersonalSpawnState;
import dev.handyspawn.state.PhantomTimer;
import dev.handyspawn.state.SessionState;
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
        sessionState = new SessionState();

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, ctx) -> {
            sessionState.onHandshakeReceived();
            HandySpawnConstants.LOG.info("Server has HandySpawn installed");
        });

        ClientPlayNetworking.registerGlobalReceiver(PersonalSpawnPayload.ID, (payload, ctx) -> {
            HandySpawnConstants.LOG.info(
                "[rx] PersonalSpawnPayload pos={} dim={} kind={} destroyed={}",
                payload.pos().map(Object::toString).orElse("empty"),
                payload.dimension().map(d -> d.identifier().toString()).orElse("empty"),
                payload.kind(), payload.isDestroyed());
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
            sessionState.onTick(msg -> HandySpawnConstants.LOG.info(msg));
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
        dev.handyspawn.render.world.WorldRenderer.register();
        dev.handyspawn.render.hud.HudRenderer.register();
    }
}
