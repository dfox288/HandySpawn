package dev.handy.mods.handyspawn.config;

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
            .option(booleanOption("personalSpawnMarker", true,
                () -> cfg.personalSpawnMarker, v -> cfg.personalSpawnMarker = v))
            .option(booleanOption("sleepReadinessTint", true,
                () -> cfg.sleepReadinessTint, v -> cfg.sleepReadinessTint = v))
            .option(booleanOption("destroyedSpawnMarker", true,
                () -> cfg.destroyedSpawnMarker, v -> cfg.destroyedSpawnMarker = v))
            .build();
    }

    private static ConfigCategory phantomCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.phantom"))
            .option(booleanOption("phantomTimer", true,
                () -> cfg.phantomTimer, v -> cfg.phantomTimer = v))
            .option(Option.<Integer>createBuilder()
                .name(Component.translatable("handyspawn.config.phantomWarningStartTick"))
                .binding(48000, () -> cfg.phantomWarningStartTick, v -> cfg.phantomWarningStartTick = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 72000))
                .build())
            .option(booleanOption("daytimeThunderstormHint", true,
                () -> cfg.daytimeThunderstormHint, v -> cfg.daytimeThunderstormHint = v))
            .build();
    }

    private static ConfigCategory navigationCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.navigation"))
            .option(booleanOption("crossDimensionReadout", true,
                () -> cfg.crossDimensionReadout, v -> cfg.crossDimensionReadout = v))
            .build();
    }

    private static ConfigCategory worldCategory(HandySpawnConfig cfg) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("handyspawn.config.category.world"))
            .option(booleanOption("worldSpawnMarker", true,
                () -> cfg.worldSpawnMarker, v -> cfg.worldSpawnMarker = v))
            .option(booleanOption("forceloadBadge", true,
                () -> cfg.forceloadBadge, v -> cfg.forceloadBadge = v))
            .option(booleanOption("forceloadBorderOverlay", false,
                () -> cfg.forceloadBorderOverlay, v -> cfg.forceloadBorderOverlay = v))
            .build();
    }

    private static Option<Boolean> booleanOption(
            String key,
            boolean defaultValue,
            java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("handyspawn.config." + key))
            .binding(defaultValue, getter::get, setter::accept)
            .controller(BooleanControllerBuilder::create)
            .build();
    }

    private ConfigScreenFactory() {}
}
