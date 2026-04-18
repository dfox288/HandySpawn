package dev.handyspawn.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import dev.handyspawn.HandySpawnConstants;

public final class HandySpawnConfig {

    private static final ConfigClassHandler<HandySpawnConfig> HANDLER =
        ConfigClassHandler.createBuilder(HandySpawnConfig.class)
            .id(Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "config"))
            .serializer(cfg -> GsonConfigSerializerBuilder.create(cfg)
                .setPath(FabricLoader.getInstance().getConfigDir()
                    .resolve(HandySpawnConstants.MOD_ID + ".json"))
                .build())
            .build();

    // --- Personal spawn ---
    @SerialEntry public boolean personalSpawnMarker = true;
    @SerialEntry public boolean sleepReadinessTint = true;
    @SerialEntry public boolean destroyedSpawnMarker = true;

    // --- Phantom awareness ---
    @SerialEntry public boolean phantomTimer = true;
    @SerialEntry public boolean daytimeThunderstormHint = true;
    @SerialEntry public int phantomWarningStartTick = 48000;

    // --- Navigation ---
    @SerialEntry public boolean crossDimensionReadout = true;

    // --- World ---
    @SerialEntry public boolean worldSpawnMarker = true;
    @SerialEntry public boolean forceloadBadge = true;
    @SerialEntry public boolean forceloadBorderOverlay = false;

    public static HandySpawnConfig get() {
        return HANDLER.instance();
    }

    public static void load() {
        HANDLER.load();
    }

    public static void save() {
        HANDLER.save();
    }

    public static ConfigClassHandler<HandySpawnConfig> handler() {
        return HANDLER;
    }

    public HandySpawnConfig() {}
}
