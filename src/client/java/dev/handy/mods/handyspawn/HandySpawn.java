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

    /**
     * Aggregates the five long-lived per-mod state objects so they can all be
     * constructed up front and exposed through a single immutable handle.
     * Built once in {@link #onInitializeClient()} before any event handler is
     * registered, so accessors below never need to null-check.
     */
    public record Context(
        CompassTargetStateController compassTarget,
        PhantomTimer phantom,
        PersonalSpawnState personal,
        SharedForceloadState forceload,
        SessionState session
    ) {}

    private static volatile Context CONTEXT;

    /**
     * Returns the live context. Throws if accessed before {@link #onInitializeClient()}
     * has run, which would indicate a startup-ordering bug rather than a legitimate
     * runtime state.
     */
    public static Context context() {
        Context c = CONTEXT;
        if (c == null) throw new IllegalStateException("HandySpawn.context() accessed before onInitializeClient");
        return c;
    }

    /**
     * Access the tracked compass-target state. This is the global compass
     * target / {@code Level.getRespawnData()} (from
     * {@code ClientboundSetDefaultSpawnPositionPacket}), not a per-player bed
     * or respawn-anchor spawn — personal spawns come through
     * {@code PersonalSpawnPayload} separately.
     */
    public static CompassTargetStateController compassTarget() {
        return context().compassTarget();
    }

    public static PhantomTimer phantom() {
        return context().phantom();
    }

    public static SharedForceloadState forceload() {
        return context().forceload();
    }

    public static PersonalSpawnState personal() {
        return context().personal();
    }

    /**
     * Per-connection session / handshake state. Renderers read
     * {@link SessionState#isServerSynced()} to choose between
     * {@link PersonalSpawnState} (server-synced) and the compass-target
     * fallback.
     */
    public static SessionState session() {
        return context().session();
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOGGER.info("HandySpawn initialising");
        HandySpawnConfig.load();

        // Phase 1: construct all state up front, so every accessor below
        // is non-null by the time any registered handler can fire.
        var dir = FabricLoader.getInstance().getConfigDir()
            .resolve(HandySpawnConstants.MOD_ID).resolve("spawns");
        Context ctx = new Context(
            new CompassTargetStateController(dir),
            PhantomTimer.create(),
            new PersonalSpawnState(),
            new SharedForceloadState(),
            new SessionState()
        );
        CONTEXT = ctx;

        // Phase 2: wire events. CompassTargetStateController registers its own
        // JOIN/DISCONNECT/tick/chunk-load handlers from `register()`.
        // Register payload types on the client side. The server calls this from
        // its own initializer; the underlying Fabric registry is keyed by payload
        // ID and deduplicates, so running it on both sides is safe and necessary.
        HandySpawnPayloads.registerAll();
        ctx.compassTarget().register();

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, c) -> {
            ctx.session().onHandshakeReceived();
            HandySpawnConstants.LOGGER.info("Server has HandySpawn installed");
        });

        ClientPlayNetworking.registerGlobalReceiver(PersonalSpawnPayload.ID, (payload, c) -> {
            ctx.personal().applyPayload(
                payload.pos(), payload.dimension(), payload.kind(), payload.isDestroyed());
        });

        ClientPlayNetworking.registerGlobalReceiver(ForceloadSyncPayload.ID, (payload, c) -> {
            ctx.forceload().update(payload.dimension(), payload.packedChunks());
        });

        // Mark the session as started on join; CompassTargetStateController
        // registers its own JOIN handler to load the persisted compass target.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ctx.session().onJoin();
        });

        // Clear transient per-connection state on disconnect so a later join
        // doesn't inherit the previous server's spawns or forceloaded chunks.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ctx.session().onDisconnect();
            ctx.personal().clear();
            ctx.forceload().clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ctx.session().onTick(msg -> HandySpawnConstants.LOGGER.info(msg));
            if (client.player == null || client.getConnection() == null) return;
            if (ctx.phantom().shouldRequestStatsThisTick(STATS_POLL_INTERVAL_TICKS)) {
                client.getConnection().send(
                    new ServerboundClientCommandPacket(
                        ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            int value = client.player.getStats().getValue(Stats.CUSTOM, Stats.TIME_SINCE_REST);
            ctx.phantom().updateFromStats(value);
        });

        // World-space rendering uses Fabric's LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN
        // so our vertices land inside vanilla's live render pass (PoseStack with
        // view matrix applied, BufferSource accepting vertices, batches not yet ended).
        dev.handy.mods.handyspawn.render.world.WorldRenderer.register();
        dev.handy.mods.handyspawn.render.hud.HudRenderer.register();
    }
}
