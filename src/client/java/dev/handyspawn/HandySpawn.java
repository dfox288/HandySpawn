package dev.handyspawn;

import net.fabricmc.api.ClientModInitializer;

public final class HandySpawn implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandySpawnConstants.LOG.info("HandySpawn initialising");
        // Wiring for state, render, and HUD is registered in later tasks.
    }
}
