package dev.handyspawn;

import net.fabricmc.api.ClientModInitializer;
import dev.handyspawn.config.HandySpawnConfig;

public final class HandySpawn implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        HandySpawnConfig.load();
    }
}
