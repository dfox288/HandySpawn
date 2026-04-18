package dev.handyspawn;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.CompassTargetStateController;
import dev.handyspawn.state.PhantomTimer;
import dev.handyspawn.state.SharedForceloadState;

public final class HandySpawn implements ClientModInitializer {

    /** Poll the server for stats roughly every 10 seconds (200 ticks @ 20tps). */
    private static final int STATS_POLL_INTERVAL_TICKS = 200;

    private static CompassTargetStateController compassTargetController;
    private static PhantomTimer phantomTimer;
    private static final SharedForceloadState FORCELOAD = new SharedForceloadState();

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
        return FORCELOAD;
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        HandySpawnConfig.load();

        var dir = FabricLoader.getInstance().getConfigDir()
            .resolve(HandySpawnConstants.MOD_ID).resolve("spawns");
        compassTargetController = new CompassTargetStateController(dir);
        compassTargetController.register();

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
