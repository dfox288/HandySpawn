package dev.handy.mods.handyspawn.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import dev.handy.mods.handyspawn.HandySpawnConstants;

public final class HandySpawnConfig {

    private static HandySpawnConfig INSTANCE;

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

    public HandySpawnConfig() {}

    public static HandySpawnConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (yaclLoaded()) {
            YaclStorage.HANDLER.load();
            INSTANCE = YaclStorage.HANDLER.instance();
        } else {
            // Without YACL the config screen can't be opened. Run with defaults; any
            // previously persisted config sits on disk and is picked up the moment YACL is installed.
            INSTANCE = new HandySpawnConfig();
        }
    }

    public static void save() {
        if (yaclLoaded()) {
            YaclStorage.HANDLER.save();
        }
    }

    private static boolean yaclLoaded() {
        return FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
    }

    /**
     * Holds the YACL handler. Inner-class loading is lazy in the JVM, so this class is only
     * resolved when {@link #yaclLoaded()} is true and we actually reference it. That keeps
     * YACL classes off the always-executed code path and avoids a {@code NoClassDefFoundError}
     * for users who run without YACL installed.
     */
    private static final class YaclStorage {
        static final ConfigClassHandler<HandySpawnConfig> HANDLER =
            ConfigClassHandler.createBuilder(HandySpawnConfig.class)
                .id(Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "config"))
                .serializer(cfg -> GsonConfigSerializerBuilder.create(cfg)
                    .setPath(FabricLoader.getInstance().getConfigDir()
                        .resolve(HandySpawnConstants.MOD_ID + ".json"))
                    .build())
                .build();
    }
}
