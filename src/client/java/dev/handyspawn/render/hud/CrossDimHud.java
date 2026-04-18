package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.CompassTargetRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class CrossDimHud {

    public static void render(GuiGraphicsExtractor ctx) {
        if (!HandySpawnConfig.get().crossDimensionReadout) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        CompassTargetRecord current = HandySpawn.compassTarget().state().current().orElse(null);
        CompassTargetRecord destroyed = HandySpawn.compassTarget().state().destroyedAt().orElse(null);
        CompassTargetRecord ref = current != null ? current : destroyed;
        if (ref == null) return;

        // Hide if the player is in the same dimension as the referenced spawn — F1/F3 handle that case
        if (ref.dimension().equals(mc.level.dimension())) return;

        String dim = simpleDimensionName(ref.dimension().identifier().toString());
        String status = current != null ? "intact" : "DESTROYED";
        int colour = current != null ? 0xFFFFFFFF : 0xFFFF5050;
        String line = "\u2302 " + dim + " \u00b7 " + status; // ⌂ prefix, · separator

        int x = 4;
        int y = 4;
        HudPrimitives.drawTextAt(ctx, Component.literal(line), x, y, colour);
    }

    private static String simpleDimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> id;
        };
    }

    private CrossDimHud() {}
}
