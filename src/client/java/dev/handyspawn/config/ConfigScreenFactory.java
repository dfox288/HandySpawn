package dev.handyspawn.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreenFactory {

    public static Screen build(Screen parent) {
        HandySpawnConfig cfg = HandySpawnConfig.get();

        YetAnotherConfigLib yacl = YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("handyspawn.config.title"))
            .category(personalCategory(cfg))
            .category(phantomCategory(cfg))
            .category(navigationCategory(cfg))
            .category(worldCategory(cfg))
            .save(HandySpawnConfig::save)
            .build();

        return yacl.generateScreen(parent);
    }

    private static ConfigCategory personalCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.personal"))
            .option(booleanOption("personalSpawnMarker",
                () -> cfg.personalSpawnMarker, v -> cfg.personalSpawnMarker = v))
            .option(booleanOption("personalSpawnThroughWalls",
                () -> cfg.personalSpawnThroughWalls, v -> cfg.personalSpawnThroughWalls = v))
            .option(booleanOption("sleepReadinessTint",
                () -> cfg.sleepReadinessTint, v -> cfg.sleepReadinessTint = v))
            .option(booleanOption("destroyedSpawnMarker",
                () -> cfg.destroyedSpawnMarker, v -> cfg.destroyedSpawnMarker = v))
            .build();
    }

    private static ConfigCategory phantomCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.phantom"))
            .option(booleanOption("phantomTimer",
                () -> cfg.phantomTimer, v -> cfg.phantomTimer = v))
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("handyspawn.config.phantomWarningStartTick"))
                .binding(48000, () -> cfg.phantomWarningStartTick, v -> cfg.phantomWarningStartTick = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 72000))
                .build())
            .option(booleanOption("daytimeThunderstormHint",
                () -> cfg.daytimeThunderstormHint, v -> cfg.daytimeThunderstormHint = v))
            .build();
    }

    private static ConfigCategory navigationCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.navigation"))
            .option(booleanOption("crossDimensionReadout",
                () -> cfg.crossDimensionReadout, v -> cfg.crossDimensionReadout = v))
            .build();
    }

    private static ConfigCategory worldCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.world"))
            .option(booleanOption("worldSpawnMarker",
                () -> cfg.worldSpawnMarker, v -> cfg.worldSpawnMarker = v))
            .option(booleanOption("forceloadBadge",
                () -> cfg.forceloadBadge, v -> cfg.forceloadBadge = v))
            .option(booleanOption("forceloadBorderOverlay",
                () -> cfg.forceloadBorderOverlay, v -> cfg.forceloadBorderOverlay = v))
            .build();
    }

    private static Option<Boolean> booleanOption(
            String key,
            java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("handyspawn.config." + key))
            .binding(true, getter::get, setter::accept)
            .controller(BooleanControllerBuilder::create)
            .build();
    }

    private ConfigScreenFactory() {}
}
