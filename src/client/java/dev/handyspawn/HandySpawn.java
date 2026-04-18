package dev.handyspawn;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.ForceloadState;
import dev.handyspawn.state.PhantomTimer;
import dev.handyspawn.state.SpawnStateController;

public final class HandySpawn implements ClientModInitializer {

    /** Poll the server for stats roughly every 10 seconds (200 ticks @ 20tps). */
    private static final int STATS_POLL_INTERVAL_TICKS = 200;

    private static SpawnStateController spawnController;
    private static PhantomTimer phantomTimer;
    private static final ForceloadState FORCELOAD = new ForceloadState();

    public static SpawnStateController spawn() {
        return spawnController;
    }

    public static PhantomTimer phantom() {
        return phantomTimer;
    }

    public static ForceloadState forceload() {
        return FORCELOAD;
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        HandySpawnConfig.load();

        var dir = FabricLoader.getInstance().getConfigDir()
            .resolve(HandySpawnConstants.MOD_ID).resolve("spawns");
        spawnController = new SpawnStateController(dir);
        spawnController.register();

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

        dev.handyspawn.render.world.WorldRenderer.register();
    }
}
