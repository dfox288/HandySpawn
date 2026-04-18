package dev.handyspawn;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.SpawnStateController;

public final class HandySpawn implements ClientModInitializer {

    private static SpawnStateController spawnController;

    public static SpawnStateController spawn() {
        return spawnController;
    }

    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        HandySpawnConfig.load();

        var dir = FabricLoader.getInstance().getConfigDir()
            .resolve(HandySpawnConstants.MOD_ID).resolve("spawns");
        spawnController = new SpawnStateController(dir);
        spawnController.register();
    }
}
